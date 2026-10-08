package com.provit.service.interview;

public class InterviewProcessingException extends IllegalStateException {
    private final boolean restartRequired;
    private final boolean answerLocked;

    public InterviewProcessingException(String message, boolean restartRequired, boolean answerLocked) {
        super(message);
        this.restartRequired = restartRequired;
        this.answerLocked = answerLocked;
    }

    public boolean isRestartRequired() { return restartRequired; }
    public boolean isAnswerLocked() { return answerLocked; }
}
