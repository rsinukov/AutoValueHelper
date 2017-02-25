package org.jetbrains.plugins.autovaluehelper.builder;

import org.jetbrains.plugins.autovaluehelper.AutoValueBaseHelperAction;

public class AutoValueBuilderAction extends AutoValueBaseHelperAction {

    public AutoValueBuilderAction() {
        super(new AutoValueBuilderHandler());
    }
}
