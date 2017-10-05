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
package com.android.tools.idea.naveditor.editor;

import com.android.SdkConstants;
import com.android.resources.ResourceFolderType;
import com.android.tools.idea.common.SyncNlModel;
import com.android.tools.idea.common.model.NlComponent;
import com.android.tools.idea.common.util.NlTreeDumper;
import com.android.tools.idea.naveditor.NavigationTestCase;
import com.android.tools.idea.naveditor.surface.NavDesignSurface;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ResourceUtil;
import icons.AndroidIcons;
import org.jetbrains.android.resourceManagers.LocalResourceManager;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import sun.awt.image.ToolkitImage;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static com.android.SdkConstants.AUTO_URI;
import static com.android.tools.idea.naveditor.NavModelBuilderUtil.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

public class AddMenuWrapperTest extends NavigationTestCase {

  private SyncNlModel myModel;
  private NavDesignSurface mySurface;
  private AddMenuWrapper myMenu;


  @Override
  public void setUp() throws Exception {
    super.setUp();
    myModel = model("nav.xml",
                    rootComponent().unboundedChildren(
                      fragmentComponent("fragment1"),
                      navigationComponent("subnav")
                        .unboundedChildren(fragmentComponent("fragment2"))))
      .build();
    mySurface = new NavDesignSurface(getProject(), getTestRootDisposable());
    mySurface.setSize(1000, 1000);
    mySurface.setModel(myModel);
    myMenu = new AddMenuWrapper(mySurface, ImmutableList.of());
    myMenu.createCustomComponentPopup();
  }

  @Override
  protected void tearDown() throws Exception {
    myModel = null;
    myMenu = null;
    super.tearDown();
  }

  public void testAddFromDestination() {
    PsiFile layout = LocalResourceManager.getInstance(myFacet.getModule()).findResourceFiles(
      ResourceFolderType.LAYOUT).stream().filter(file -> file.getName().equals("activity_main.xml")).findFirst().get();
    NavActionManager.Destination destination = new NavActionManager.Destination(
      (XmlFile)layout, "MainActivity", "mytest.navtest.MainActivity", "activity", null);
    myMenu.addElement(destination, mySurface, null, null);
    assertEquals("NlComponent{tag=<navigation>, instance=0}\n" +
                 "    NlComponent{tag=<fragment>, instance=1}\n" +
                 "    NlComponent{tag=<navigation>, instance=2}\n" +
                 "        NlComponent{tag=<fragment>, instance=3}\n" +
                 "    NlComponent{tag=<activity>, instance=4}",
                 new NlTreeDumper().toTree(myModel.getComponents()));
    NlComponent newChild = myModel.getComponents().get(0).getChild(2);
    assertEquals(SdkConstants.LAYOUT_RESOURCE_PREFIX + "activity_main",
                 newChild.getAttribute(SdkConstants.TOOLS_URI, SdkConstants.ATTR_LAYOUT));
    assertEquals("mytest.navtest.MainActivity",
                 newChild.getAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_NAME));
    assertEquals("@+id/mainActivity",
                 newChild.getAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_ID));
  }

  public void testAddDirectly() {
    myMenu.addElement(mySurface, "myTag", "myId", "myName", component -> component.setAttribute("ns", "attr", "value"));
    assertEquals("NlComponent{tag=<navigation>, instance=0}\n" +
                 "    NlComponent{tag=<fragment>, instance=1}\n" +
                 "    NlComponent{tag=<navigation>, instance=2}\n" +
                 "        NlComponent{tag=<fragment>, instance=3}\n" +
                 "    NlComponent{tag=<myTag>, instance=4}",
                 new NlTreeDumper().toTree(myModel.getComponents()));
    NlComponent newChild = myModel.find("myId");
    assertEquals(ImmutableList.of(newChild), mySurface.getSelectionModel().getSelection());
    assertEquals("myName", newChild.getAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_NAME));
    assertEquals("@+id/myId", newChild.getAttribute(SdkConstants.ANDROID_URI, SdkConstants.ATTR_ID));
    assertEquals("value", newChild.getAttribute("ns", "attr"));
  }

  public void testFragmentValidation() {
    myMenu.myKindPopup.setSelectedItem("fragment");
    myMenu.myIdField.setText("myId");
    myMenu.myLabelField.setText("myLabel");
    assertTrue(myMenu.validate());
    assertFalse(myMenu.myValidationLabel.isVisible());

    myMenu.myIdField.setText("");
    assertFalse(myMenu.validate());
    assertTrue(myMenu.myValidationLabel.isVisible());
    myMenu.myIdField.setText("myId");

    assertTrue(myMenu.validate());
    assertFalse(myMenu.myValidationLabel.isVisible());

    myMenu.myLabelField.setText("");
    assertFalse(myMenu.validate());
    assertTrue(myMenu.myValidationLabel.isVisible());
  }

  public void testActivityValidation() {
    myMenu.myKindPopup.setSelectedItem("activity");
    myMenu.myIdField.setText("myId");
    myMenu.myLabelField.setText("myLabel");
    assertTrue(myMenu.validate());
    assertFalse(myMenu.myValidationLabel.isVisible());

    myMenu.myIdField.setText("");
    assertFalse(myMenu.validate());
    assertTrue(myMenu.myValidationLabel.isVisible());
    myMenu.myIdField.setText("myId");

    assertTrue(myMenu.validate());
    assertFalse(myMenu.myValidationLabel.isVisible());

    myMenu.myLabelField.setText("");
    assertFalse(myMenu.validate());
    assertTrue(myMenu.myValidationLabel.isVisible());
  }

  public void testNestedValidation() {
    myMenu.myKindPopup.setSelectedItem("navigation");
    myMenu.myIdField.setText("myId");
    myMenu.myLabelField.setText("myLabel");
    assertTrue(myMenu.validate());
    assertFalse(myMenu.myValidationLabel.isVisible());

    myMenu.myIdField.setText("");
    assertFalse(myMenu.validate());
    assertTrue(myMenu.myValidationLabel.isVisible());
    myMenu.myIdField.setText("myId");

    assertTrue(myMenu.validate());
    assertFalse(myMenu.myValidationLabel.isVisible());

    myMenu.myLabelField.setText("");
    assertFalse(myMenu.validate());
    assertTrue(myMenu.myValidationLabel.isVisible());
  }

  public void testIncludeValidation() {
    myMenu.myKindPopup.setSelectedItem("include");
    myMenu.myIdField.setText("myId");
    myMenu.myLabelField.setText("myLabel");
    // Not possible to have an invalid value in "source"

    assertTrue(myMenu.validate());
    assertFalse(myMenu.myValidationLabel.isVisible());

    myMenu.myIdField.setText("");
    assertFalse(myMenu.validate());
    assertTrue(myMenu.myValidationLabel.isVisible());
    myMenu.myIdField.setText("myId");

    assertTrue(myMenu.validate());
    assertFalse(myMenu.myValidationLabel.isVisible());

    myMenu.myLabelField.setText("");
    assertFalse(myMenu.validate());
    assertTrue(myMenu.myValidationLabel.isVisible());
  }

  public void testVisibleComponents() {
    myMenu.myKindPopup.setSelectedItem("fragment");
    assertFalse(myMenu.mySourcePopup.isVisible());
    assertFalse(myMenu.mySourceLabel.isVisible());
    assertTrue(myMenu.myIdField.isVisible());
    assertTrue(myMenu.myLabelField.isVisible());
    assertTrue(myMenu.myLabelLabel.isVisible());
    assertTrue(myMenu.myIdLabel.isVisible());

    myMenu.myKindPopup.setSelectedItem("navigation");
    assertFalse(myMenu.mySourcePopup.isVisible());
    assertFalse(myMenu.mySourceLabel.isVisible());
    assertTrue(myMenu.myIdField.isVisible());
    assertTrue(myMenu.myLabelField.isVisible());
    assertTrue(myMenu.myLabelLabel.isVisible());
    assertTrue(myMenu.myIdLabel.isVisible());

    myMenu.myKindPopup.setSelectedItem("include");
    assertTrue(myMenu.mySourcePopup.isVisible());
    assertTrue(myMenu.mySourceLabel.isVisible());
    assertFalse(myMenu.myIdField.isVisible());
    assertFalse(myMenu.myLabelField.isVisible());
    assertFalse(myMenu.myLabelLabel.isVisible());
    assertFalse(myMenu.myIdLabel.isVisible());

    myMenu.myKindPopup.setSelectedItem("activity");
    assertFalse(myMenu.mySourcePopup.isVisible());
    assertFalse(myMenu.mySourceLabel.isVisible());
    assertTrue(myMenu.myIdField.isVisible());
    assertTrue(myMenu.myLabelField.isVisible());
    assertTrue(myMenu.myLabelLabel.isVisible());
    assertTrue(myMenu.myIdLabel.isVisible());
  }

  public void testCreateNested() {
    AddMenuWrapper menu = Mockito.spy(myMenu);

    menu.myKindPopup.setSelectedItem("navigation");
    menu.myIdField.setText("myId");
    menu.myLabelField.setText("myLabel");

    menu.createDestination();

    Mockito.verify(menu).addElement(mySurface, "navigation", "myId", "myLabel", null);
  }

  public void testCreateFragment() {
    AddMenuWrapper menu = Mockito.spy(myMenu);

    menu.myKindPopup.setSelectedItem("fragment");
    menu.myIdField.setText("myId");
    menu.myLabelField.setText("myLabel");

    menu.createDestination();

    Mockito.verify(menu)
      .addElement(new NavActionManager.Destination(null, "", "", "fragment", null), mySurface,
                  "myId", "myLabel");
  }

  public void testCreateActivity() {
    AddMenuWrapper menu = Mockito.spy(myMenu);

    menu.myKindPopup.setSelectedItem("activity");
    menu.myIdField.setText("myId");
    menu.myLabelField.setText("myLabel");

    menu.createDestination();

    Mockito.verify(menu)
      .addElement(new NavActionManager.Destination(null, "", "", "activity", null), mySurface,
                  "myId", "myLabel");
  }

  public void testCreateInclude() {
    AddMenuWrapper menu = Mockito.spy(myMenu);

    menu.myKindPopup.setSelectedItem("include");
    menu.myIdField.setText("myId");
    menu.myLabelField.setText("myLabel");
    menu.mySourcePopup.setSelectedItem("navigation.xml");

    menu.createDestination();

    ArgumentCaptor<Consumer<NlComponent>> consumerArg = ArgumentCaptor.forClass(Consumer.class);
    Mockito.verify(menu)
      .addElement(eq(mySurface), eq("include"), isNull(), isNull(), consumerArg.capture());

    NlComponent component = Mockito.mock(NlComponent.class);
    consumerArg.getValue().accept(component);
    Mockito.verify(component).setAttribute(AUTO_URI, "graph", "@navigation/navigation");
  }

  public void testUniqueId() {
    myMenu.myKindPopup.setSelectedItem("fragment");
    assertEquals("fragment", myMenu.myIdField.getText());
    myMenu.createDestination();
    myMenu = new AddMenuWrapper(mySurface, ImmutableList.of());
    myMenu.createCustomComponentPopup();
    myMenu.myKindPopup.setSelectedItem("fragment");
    assertEquals("fragment2", myMenu.myIdField.getText());
  }

  public void testImageLoading() throws Exception {
    Lock lock = new ReentrantLock();
    lock.lock();

    // use createImage so the instances are different
    ToolkitImage image = (ToolkitImage)Toolkit.getDefaultToolkit().createImage(
      ResourceUtil.getResource(AndroidIcons.class, "/icons/naveditor", "basic-activity.png"));
    image.preload((img, infoflags, x, y, width, height) -> {
      lock.lock();
      return false;
    });

    MediaTracker tracker = new MediaTracker(new JPanel());
    tracker.addImage(image, 0);

    NavActionManager.Destination dest = new NavActionManager.Destination(null, "foo", "foo", "fragment", image);

    AddMenuWrapper menu = new AddMenuWrapper(mySurface, ImmutableList.of(dest));
    menu.createCustomComponentPopup();
    assertTrue(menu.myLoadingPanel.isLoading());
    lock.unlock();
    tracker.waitForAll();
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    assertFalse(menu.myLoadingPanel.isLoading());

    // Now images are loaded, make sure a new menu doesn't even have the loading panel
    menu = new AddMenuWrapper(mySurface, ImmutableList.of(dest));
    menu.createCustomComponentPopup();
    assertNull(menu.myLoadingPanel);
  }

  public void testKindPopup() {
    ComboBox<String> popup = myMenu.myKindPopup;
    ListCellRenderer<? super String> renderer = popup.getRenderer();
    Set<String> result = new HashSet<>();
    for (int i = 0; i < popup.getItemCount(); i++) {
      result.add(((JLabel)renderer.getListCellRendererComponent(null, popup.getItemAt(i), i, false, false)).getText());
    }
    assertEquals(ImmutableSet.of("Include Graph", "Nested Graph", "Fragment", "Activity"), result);
    assertEquals("fragment", popup.getSelectedItem());
  }
}
