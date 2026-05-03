package io.audita.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerSecurityAnnotationsTest {

    @Test
    void userControllerAppliesReadOnlyAuditorPolicy() {
        Map<String, String> preAuthByMethod = readPreAuthorizeByMethod(UserController.class);

        assertThat(preAuthByMethod.get("listUsers")).contains("AUDITOR");
        assertThat(preAuthByMethod.get("getUser")).contains("AUDITOR");

        assertThat(preAuthByMethod.get("inviteUser")).doesNotContain("AUDITOR");
        assertThat(preAuthByMethod.get("updateUser")).doesNotContain("AUDITOR");
        assertThat(preAuthByMethod.get("deactivateUser")).doesNotContain("AUDITOR");
        assertThat(preAuthByMethod.get("reactivateUser")).doesNotContain("AUDITOR");
    }

    @Test
    void mutatingControllersKeepAuditorOutOfWriteEndpoints() {
        assertMutatingMethodsExcludeAuditor(GroupController.class, Set.of(
                "createGroup", "updateGroup", "deleteGroup", "addMember", "removeMember"
        ));

        assertMutatingMethodsExcludeAuditor(ChangeRequestController.class, Set.of(
                "create", "update", "submit", "cancel",
                "addApprover", "reorderApprovers", "removeApprover",
                "approve", "reject", "upsertCustomFields", "uploadAttachment"
        ));
    }

    private void assertMutatingMethodsExcludeAuditor(Class<?> controller, Set<String> methods) {
        Map<String, String> preAuthByMethod = readPreAuthorizeByMethod(controller);
        for (String methodName : methods) {
            assertThat(preAuthByMethod.get(methodName))
                    .as("Method %s.%s should exclude AUDITOR", controller.getSimpleName(), methodName)
                    .doesNotContain("AUDITOR");
        }
    }

    private Map<String, String> readPreAuthorizeByMethod(Class<?> controllerClass) {
        return java.util.Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PreAuthorize.class))
                .collect(java.util.stream.Collectors.toMap(
                        Method::getName,
                        m -> m.getAnnotation(PreAuthorize.class).value(),
                        (left, right) -> left
                ));
    }
}
