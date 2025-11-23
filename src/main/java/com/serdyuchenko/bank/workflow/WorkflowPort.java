package com.serdyuchenko.bank.workflow;

import com.serdyuchenko.bank.domain.User;

/**
 * Abstraction for orchestrating workflow steps (e.g., Camunda) when domain events occur.
 */
public interface WorkflowPort {
    /**
     * Initiates onboarding or related workflow for a new user.
     *
     * @param user domain user being onboarded
     */
    void startOnboarding(User user);
}
