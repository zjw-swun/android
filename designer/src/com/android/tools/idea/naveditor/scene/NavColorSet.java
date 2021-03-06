/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.tools.idea.naveditor.scene;

import com.android.tools.idea.uibuilder.handlers.constraint.drawing.ColorSet;
import com.android.tools.idea.uibuilder.handlers.constraint.drawing.decorator.WidgetDecorator;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import java.awt.Color;

/**
 * {@link ColorSet} for the navigation editor.
 */
public class NavColorSet extends ColorSet {
  public static final JBColor FRAME_COLOR = new JBColor(0xa7a7a7, 0x2d2f31);
  public static final JBColor HIGHLIGHTED_FRAME_COLOR = new JBColor(0xa7a7a7, 0xa1a1a1);
  public static final JBColor SELECTED_FRAME_COLOR = new JBColor(0x1886f7, 0x9ccdff);
  public static final JBColor SUBDUED_FRAME_COLOR = new JBColor(0xa7a7a7, 0xa1a1a1);
  public static final JBColor BACKGROUND_COLOR = new JBColor(0xf5f5f5, 0x2d2f31);
  public static final JBColor SUBDUED_BACKGROUND_COLOR = new JBColor(0xfcfcfc, 0x313435);
  public static final JBColor COMPONENT_BACKGROUND_COLOR = new JBColor(0xfafafa, 0x515658);
  public static final JBColor TEXT_COLOR = new JBColor(0xa7a7a7, 0x888888);
  public static final JBColor SELECTED_TEXT_COLOR = new JBColor(0x1886f7, 0x9ccdff);
  public static final JBColor SUBDUED_TEXT_COLOR = new JBColor(0x656565, 0xbababb);
  public static final JBColor ACTION_COLOR = new JBColor(new Color(0xb2a7a7a7, true), new Color(0xb2888888, true));
  public static final JBColor HIGHLIGHTED_ACTION_COLOR = new JBColor(0xa7a7a7, 0x888888);
  public static final JBColor SELECTED_ACTION_COLOR = new JBColor(0x1886f7, 0x9ccdff);
  public static final JBColor ACTIVITY_BORDER_COLOR = new JBColor(0xa7a7a7, 0x2d2f31);
  // Note that this must match the highlight color of ActionButtonWithText
  public static final JBColor LIST_MOUSEOVER_COLOR = new JBColor(Gray.xDB, new Color(0x55595c));
  public static final JBColor PLACEHOLDER_BORDER_COLOR = new JBColor(0xcccccc, 0x3f4244);
  public static final JBColor PLACEHOLDER_TEXT_COLOR = new JBColor(0xcccccc, 0x888888);
  public static final JBColor PLACEHOLDER_BACKGROUND_COLOR = new JBColor(0xfdfdfd, 0x515658);

  private Color mActions;
  private Color mHighlightedActions;
  private Color mSelectedActions;

  public NavColorSet() {
    mStyle = WidgetDecorator.ANDROID_STYLE;
    mDrawBackground = true;
    mDrawWidgetInfos = false;

    mFrames = FRAME_COLOR;
    mHighlightedFrames = HIGHLIGHTED_FRAME_COLOR;
    mSelectedFrames = SELECTED_FRAME_COLOR;
    mSubduedFrames = SUBDUED_FRAME_COLOR;

    mBackground = BACKGROUND_COLOR;
    mSubduedBackground = SUBDUED_BACKGROUND_COLOR;
    mComponentBackground = COMPONENT_BACKGROUND_COLOR;

    mText = TEXT_COLOR;
    mSelectedText = SELECTED_TEXT_COLOR;
    mSubduedText = SUBDUED_TEXT_COLOR;

    mActions = ACTION_COLOR;
    mHighlightedActions = HIGHLIGHTED_ACTION_COLOR;
    mSelectedActions = SELECTED_ACTION_COLOR;

    mLassoSelectionBorder = DEFAULT_LASSO_BORDER_COLOR;
    mLassoSelectionFill = DEFAULT_LASSO_FILL_COLOR;
  }

  public Color getActions()  {
    return mActions;
  }

  public Color getHighlightedActions()  {
    return mHighlightedActions;
  }

  public Color getSelectedActions() {
    return mSelectedActions;
  }
}
