package com.serdyuchenko.bank.workflow.inmemory;

import org.springframework.stereotype.Component;

import com.serdyuchenko.bank.domain.User;
import com.serdyuchenko.bank.workflow.WorkflowPort;

/**
 * Placeholder adapter that satisfies the {@link WorkflowPort} contract without invoking any engine.
 */
@Component
public class NoopWorkflowAdapter implements WorkflowPort {

    @Override
    public void startOnboarding(User user) {
        // No-op
    }

}
