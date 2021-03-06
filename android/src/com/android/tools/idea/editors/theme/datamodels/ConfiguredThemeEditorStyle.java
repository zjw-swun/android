/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.android.tools.idea.editors.theme.datamodels;

import com.android.SdkConstants;
import com.android.ide.common.rendering.api.*;
import com.android.ide.common.resources.ResourceItem;
import com.android.ide.common.resources.ResourceRepository;
import com.android.ide.common.resources.ResourceResolver;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.resources.ResourceType;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.editors.theme.ResolutionUtils;
import com.android.tools.idea.editors.theme.ThemeEditorContext;
import com.android.tools.idea.editors.theme.ThemeResolver;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class represents styles in ThemeEditor. In addition to {@link ThemeEditorStyle},
 * it knows about current {@link Configuration} used in ThemeEditor.
 * TODO: Move Configuration independent methods to ThemeEditorStyle.
 */
public class ConfiguredThemeEditorStyle extends ThemeEditorStyle {
  private final @NotNull StyleResourceValueImpl myStyleResourceValue;
  private final @NotNull Configuration myConfiguration;

  /**
   * Source module of the theme, set to null if the theme comes from external libraries or the framework.
   * For currently edited theme stored in {@link ThemeEditorContext#getCurrentContextModule()}.
   */
  private final @Nullable Module mySourceModule;

  public ConfiguredThemeEditorStyle(@NotNull Configuration configuration,
                                    @NotNull StyleResourceValue styleResourceValue,
                                    @Nullable Module sourceModule) {
    super(configuration.getConfigurationManager(), styleResourceValue.asReference());
    myStyleResourceValue = StyleResourceValueImpl.copyOf(styleResourceValue);
    myConfiguration = configuration;
    mySourceModule = sourceModule;
  }

  /**
   * Returns the url representation of this style. The result will start either with
   * {@value SdkConstants#ANDROID_STYLE_RESOURCE_PREFIX} or {@value SdkConstants#STYLE_RESOURCE_PREFIX}.
   */
  @NotNull
  public String getStyleResourceUrl() {
    return myStyleResourceValue.getResourceUrl().toString();
  }

  /**
   * Returns StyleResourceValueImpl for the current Configuration.
   */
  @NotNull
  public StyleResourceValueImpl getStyleResourceValue() {
    return myStyleResourceValue;
  }

  /**
   * Returns whether this style is editable.
   */
  public boolean isReadOnly() {
    return !isProjectStyle();
  }

  /**
   * Returns all the style attributes and its values. For each attribute, multiple {@link ConfiguredElement} can be returned
   * representing the multiple values in different configurations for each item.
   * TODO: needs to be deleted, as we don't use this method except tests
   */
  @NotNull
  public ImmutableCollection<ConfiguredElement<StyleItemResourceValue>> getConfiguredValues() {
    // Get a list of all the items indexed by the item name. Each item contains a list of the possible
    // values in this theme in different configurations.
    //
    // If item1 has multiple values in different configurations, there will be an
    // item1 = {folderConfiguration1 -> value1, folderConfiguration2 -> value2}
    final ImmutableList.Builder<ConfiguredElement<StyleItemResourceValue>> itemResourceValues = ImmutableList.builder();

    if (isFramework()) {
      ResourceRepository frameworkResources = myConfiguration.getFrameworkResources();
      assert frameworkResources != null;

      List<ResourceItem> styleItems =
          frameworkResources.getResources(ResourceNamespace.ANDROID, ResourceType.STYLE, myStyleResourceValue.getName());
      for (ResourceItem item : styleItems) {
        ResourceValue styleResourceValue = item.getResourceValue();

        if (styleResourceValue instanceof StyleResourceValue) {
          FolderConfiguration folderConfiguration = item.getConfiguration();
          for (StyleItemResourceValue value : ((StyleResourceValue)styleResourceValue).getDefinedItems()) {
            itemResourceValues.add(ConfiguredElement.create(folderConfiguration, value));
          }
        }
      }
    }
    else {
      for (ResourceItem styleDefinition : getStyleResourceItems()) {
        ResourceValue styleResourceValue = styleDefinition.getResourceValue();
        FolderConfiguration folderConfiguration = styleDefinition.getConfiguration();

        if (styleResourceValue instanceof StyleResourceValue) {
          for (StyleItemResourceValue value : ((StyleResourceValue)styleResourceValue).getDefinedItems()) {
            // We use the qualified name since apps and libraries can use the same attribute name twice with and without "android:"
            itemResourceValues.add(ConfiguredElement.create(folderConfiguration, value));
          }
        }
      }
    }

    return itemResourceValues.build();
  }

  /**
   * Returns the names of all the parents of this style. Parents might differ depending on the folder configuration, this returns all the
   * variants for this style.
   */
  // TODO(namespaces): Change return type to Collection<ConfiguredElement<ResourceReference>>.
  public Collection<ConfiguredElement<String>> getParentNames() {
    if (isFramework()) {
      // Framework themes do not have multiple parents so we just get the only one.
      ConfiguredThemeEditorStyle parent = getParent();
      if (parent != null) {
        return ImmutableList.of(ConfiguredElement.create(new FolderConfiguration(), parent.getQualifiedName()));
      }
      // The theme has no parent (probably the main "Theme" style).
      return Collections.emptyList();
    }

    ImmutableList.Builder<ConfiguredElement<String>> parents = ImmutableList.builder();
    for (ResourceItem styleItem : getStyleResourceItems()) {
      StyleResourceValue style = (StyleResourceValue)styleItem.getResourceValue();
      assert style != null;
      String parentName = ResolutionUtils.getParentQualifiedName(style);
      if (parentName != null) {
        parents.add(ConfiguredElement.create(styleItem.getConfiguration(), parentName));
      }
    }
    return parents.build();
  }

  public boolean hasItem(@Nullable EditedStyleItem item) {
    //TODO: add isOverriden() method to EditedStyleItem
    ResourceReference attrReference = item == null ? null : item.getAttrReference();
    return attrReference != null && getStyleResourceValue().getItem(attrReference) != null;
  }

  public StyleItemResourceValue getItem(@NotNull String name, boolean isFramework) {
    // TODO: namespaces
    return getStyleResourceValue().getItem(ResourceNamespace.fromBoolean(isFramework), name);
  }

  /**
   * See {@link #getParent(ThemeResolver)}
   */
  public ConfiguredThemeEditorStyle getParent() {
    return getParent(null);
  }

  /**
   * @param themeResolver theme resolver that would be used to look up parent theme by name
   *                      Pass null if you don't care about resulting ThemeEditorStyle source module (which would be null in that case)
   * @return the style parent
   */
  @Nullable/*if this is a root style*/
  public ConfiguredThemeEditorStyle getParent(@Nullable ThemeResolver themeResolver) {
    ResourceResolver resolver = myConfiguration.getResourceResolver();
    assert resolver != null;

    StyleResourceValue parent = resolver.getParent(getStyleResourceValue());
    if (parent == null) {
      return null;
    }

    if (themeResolver == null) {
      return ResolutionUtils.getThemeEditorStyle(myConfiguration, parent.asReference(), null);
    }
    else {
      return themeResolver.getTheme(parent.asReference());
    }
  }

  @Override
  public String toString() {
    if (!isReadOnly()) {
      return "[" + getName() + "]";
    }

    return getName();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ConfiguredThemeEditorStyle)) {
      return false;
    }

    return getStyleReference().equals(((ConfiguredThemeEditorStyle)obj).getStyleReference());
  }

  @Override
  public int hashCode() {
    return getStyleReference().hashCode();
  }

  @NotNull
  public Configuration getConfiguration() {
    return myConfiguration;
  }

  /**
   * Plain getter, see {@link #mySourceModule} for field description.
   */
  @Nullable
  public final Module getSourceModule() {
    return mySourceModule;
  }
}
