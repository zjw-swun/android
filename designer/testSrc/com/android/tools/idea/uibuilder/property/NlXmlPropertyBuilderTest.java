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
package com.android.tools.idea.uibuilder.property;

import com.android.tools.adtui.ptable.PTableItem;
import com.android.tools.adtui.ptable.PTableModel;
import com.android.tools.idea.common.model.NlComponent;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.util.ui.UIUtil;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import static com.android.SdkConstants.*;
import static com.google.common.truth.Truth.assertThat;

public class NlXmlPropertyBuilderTest extends PropertyTestCase {
  private PTableModel myModel;
  private NlPTable myTable;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    myFixture.addFileToProject("res/values/strings.xml", getStrings());
    myFixture.addFileToProject("res/values/dimens.xml", getDimens());
    myModel = new PTableModel();
    myTable = new NlPTable(myModel);
  }

  @Override
  public void tearDown() throws Exception {
    try {
      super.tearDown();
    }
    finally {
      myModel = null;
      myTable = null;
    }
  }

  public void testTextView() {
    setProperties(myTextView);
    NlXmlPropertyBuilder builder = createBuilder(myTextView);
    builder.build();
    int rows = myModel.getRowCount();
    int index = 0;
    checkHeader(index++, "layout/merge.xml");
    checkProperty(index++, ANDROID_URI, ATTR_LAYOUT_WIDTH);
    checkProperty(index++, ANDROID_URI, ATTR_LAYOUT_HEIGHT);
    checkProperty(index++, ANDROID_URI, ATTR_ELEVATION);
    checkProperty(index++, ANDROID_URI, ATTR_ID);
    checkProperty(index++, ANDROID_URI, ATTR_PADDING_BOTTOM);
    checkProperty(index++, ANDROID_URI, ATTR_TEXT);
    checkProperty(index++, ANDROID_URI, ATTR_TEXT_COLOR);
    checkAddProperty(index++);
    checkHeader(index++, "values/dimens.xml");
    checkResourceItem(index++, "bottom", "35dp");
    checkHeader(index++, "values/strings.xml");
    checkResourceItem(index++, "hello_world", "Hello World!");
    assertThat(rows).isEqualTo(index);
  }

  private void checkHeader(int rowIndex, @NotNull String expectedHeader) {
    PTableItem item = myTable.getItemAt(rowIndex);
    assertThat(item).isInstanceOf(NlResourceHeader.class);
    assertThat(item).isNotNull();
    assertThat(item.getName()).isEqualTo(expectedHeader);
  }

  private void checkProperty(int rowIndex, @NotNull String namespace, @NotNull String attribute) {
    PTableItem item = myTable.getItemAt(rowIndex);
    assertThat(item).isInstanceOf(NlPropertyItem.class);
    NlPropertyItem propertyItem = (NlPropertyItem)item;
    assertThat(propertyItem).isNotNull();
    assertThat(propertyItem.getNamespace()).isEqualTo(namespace);
    assertThat(propertyItem.getName()).isEqualTo(attribute);
  }

  @SuppressWarnings("SameParameterValue")
  private void checkAddProperty(int rowIndex) {
    PTableItem item = myTable.getItemAt(rowIndex);
    assertThat(item).isInstanceOf(AddPropertyItem.class);
  }

  private void checkResourceItem(int rowIndex, @NotNull String name, @NotNull String value) {
    PTableItem item = myTable.getItemAt(rowIndex);
    assertThat(item).isInstanceOf(NlResourceItem.class);
    NlResourceItem resourceItem = (NlResourceItem)item;
    assertThat(resourceItem).isNotNull();
    assertThat(resourceItem.getName()).isEqualTo(name);
    assertThat(resourceItem.getValue()).isEqualTo(value);
  }

  @NotNull
  private NlXmlPropertyBuilder createBuilder(@NotNull NlComponent... componentArray) {
    List<NlComponent> components = Arrays.asList(componentArray);
    return new NlXmlPropertyBuilder(myPropertiesManager, myTable, components, getPropertyTable(components));
  }

  @Language("XML")
  private static String getStrings() {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
           "<resources>\n" +
           "    <string name=\"app_name\">My Application</string>\n" +
           "    <string name=\"hello_world\">Hello World!</string>\n" +
           "</resources>\n";
  }

  @Language("XML")
  private static String getDimens() {
    return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
           "<resources>\n" +
           "    <dimen name=\"bottom\">35dp</dimen>\n" +
           "    <dimen name=\"top\">20dp</dimen>\n" +
           "</resources>\n";
  }

  private void setProperties(@NotNull NlComponent textView) {
    new WriteCommandAction.Simple(getProject(), "Set Text property") {
      @Override
      protected void run() throws Throwable {
        textView.setAttribute(ANDROID_URI, ATTR_TEXT, "@string/hello_world");
        textView.setAttribute(ANDROID_URI, ATTR_PADDING_BOTTOM, "@dimen/bottom");
      }
    }.execute();
    UIUtil.dispatchAllInvocationEvents();
  }
}
