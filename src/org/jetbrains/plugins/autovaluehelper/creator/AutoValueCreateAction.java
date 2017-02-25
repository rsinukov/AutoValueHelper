package org.jetbrains.plugins.autovaluehelper.creator;

import org.jetbrains.plugins.autovaluehelper.AutoValueBaseHelperAction;

public class AutoValueCreateAction extends AutoValueBaseHelperAction {

    public AutoValueCreateAction() {
        super(new AutoValueCreateHandler());
    }
}
