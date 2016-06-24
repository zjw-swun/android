/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.uibuilder.surface;

import com.android.tools.idea.uibuilder.mockup.Mockup;
import com.android.tools.idea.uibuilder.model.Coordinates;
import com.android.tools.idea.uibuilder.model.ModelListener;
import com.android.tools.idea.uibuilder.model.NlComponent;
import com.android.tools.idea.uibuilder.model.NlModel;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Layer build to be on top of the BluePrint ScreenView displaying
 * an optional mockup of the layout to be build
 *
 * @See MockupModel
 **/
public class MockupLayer extends Layer {

  private final ScreenView myScreenView;
  private Dimension myScreenViewSize = new Dimension();
  private Rectangle myComponentSwingBounds = new Rectangle();
  private NlModel myNlModel;
  private List<Mockup> myMockups;

  public MockupLayer(ScreenView screenView) {
    assert screenView != null;
    myScreenView = screenView;
    myScreenViewSize = myScreenView.getSize(myScreenViewSize);
    setNlModel(myScreenView.getModel());
  }

  public void setNlModel(NlModel nlModel) {
    myNlModel = nlModel;
    myMockups = Mockup.createAll(myNlModel);
    myNlModel.addListener(new ModelListener() {
      @Override
      public void modelChanged(@NotNull NlModel model) {
        setNlModel(model);
      }

      @Override
      public void modelRendered(@NotNull NlModel model) {
      }
    });
  }

  @NotNull
  public List<Mockup> getMockups() {
    return myMockups;
  }

  @Override
  public void paint(@NotNull Graphics2D g) {
    if (!myScreenView.getSurface().isMockupVisible()
        || myMockups.isEmpty()) {
      return;
    }
    final Composite composite = g.getComposite();
    myScreenViewSize = myScreenView.getSize(myScreenViewSize);

    for (int i = 0; i < myMockups.size(); i++) {
      final Mockup mockup = myMockups.get(i);
      paintMockup(g, mockup);
    }
    g.setComposite(composite);
  }

  private void paintMockup(@NotNull Graphics2D g, Mockup mockup) {
    final BufferedImage image = mockup.getImage();
    if (image != null) {
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, mockup.getAlpha()));
      // Coordinates of the component in the ScreenView system
      int componentSwingX = Coordinates.getSwingX(myScreenView, mockup.getComponent().x);
      int componentSwingY = Coordinates.getSwingY(myScreenView, mockup.getComponent().y);
      int componentSwingW = Coordinates.getSwingDimension(myScreenView, mockup.getComponent().w);
      int componentSwingH = Coordinates.getSwingDimension(myScreenView, mockup.getComponent().h);

      myComponentSwingBounds.setBounds(
        componentSwingX,
        componentSwingY,
        componentSwingW,
        componentSwingH);

      final Rectangle dest = mockup.getBounds(myScreenView, null);
      final Rectangle src = mockup.getCropping();
      src.width = src.width <= 0 ? image.getWidth() : src.width;
      src.height = src.height <= 0 ? image.getHeight() : src.height;

      if (!mockup.isFullScreen()) {
        Rectangle2D.intersect(dest, myComponentSwingBounds, dest);
      }
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g.drawImage(image,
                  dest.x, dest.y, dest.x + dest.width, dest.y + dest.height,
                  src.x, src.y, src.x + src.width, src.y + src.height,
                  null);
    }
  }
}