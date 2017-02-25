package org.jetbrains.plugins.autovaluehelper;

import com.intellij.codeInsight.generation.PsiMethodMember;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public abstract class AutoValueBaseHelperGenerator implements Runnable {

    protected static final String NONNULL = "android.support.annotation.NonNull";
    protected static final String NULLABLE = "android.support.annotation.Nullable";
    protected static final String AUTO_VALUE_BUILDER = "com.google.auto.value.AutoValue.Builder";

    protected final List<PsiMethodMember> selectedMethods;
    protected final Project project;
    protected final PsiElementFactory psiElementFactory;

    protected AutoValueBaseHelperGenerator(@NotNull List<PsiMethodMember> selectedMethods, @NotNull Project project) {
        this.selectedMethods = selectedMethods;
        this.project = project;
        this.psiElementFactory = JavaPsiFacade.getInstance(project).getElementFactory();
    }

    @NotNull
    protected PsiParameter createSetterParameter(
            @NotNull PsiMethod getterMethod,
            @NotNull PsiType parameterType,
            @NotNull String parameterName
    ) {
        final PsiParameter setterParameter = psiElementFactory.createParameter(parameterName, parameterType);
        if (!(parameterType instanceof PsiPrimitiveType)) {
            PsiModifierList modifierList = setterParameter.getModifierList();
            assert modifierList != null;
            if (getterMethod.getModifierList().findAnnotation(NULLABLE) != null) {
                modifierList.addAnnotation(NULLABLE);
            } else if (getterMethod.getModifierList().findAnnotation(NONNULL) != null) {
                modifierList.addAnnotation(NONNULL);
            }
        }
        return setterParameter;
    }

    @NotNull
    protected PsiElement addMethod(
            @NotNull final PsiClass target,
            @Nullable final PsiElement after,
            @NotNull final PsiMethod newMethod
    ) {
        PsiMethod existingMethod = target.findMethodBySignature(newMethod, false);

        if (existingMethod == null) {
            if (after != null) {
                return target.addAfter(newMethod, after);
            } else {
                return target.add(newMethod);
            }
        }

        existingMethod.replace(newMethod);
        return existingMethod;
    }

    @NotNull
    protected String getAutoValueClassName(@NotNull PsiClass targetClass) {
        StringBuilder classNameBuilder = new StringBuilder();
        PsiClass currentClass = targetClass;
        while (currentClass != null) {
            final String currentClassName = currentClass.getName();
            classNameBuilder.insert(0, currentClassName).insert(0, "_");
            currentClass = currentClass.getContainingClass();
        }
        classNameBuilder.insert(0, "AutoValue");
        return classNameBuilder.toString();
    }
}
