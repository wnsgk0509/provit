const PREFIX = 'provit:interview-progress:';
export const MAINTENANCE_MESSAGE = '모의면접 서비스 점검 중입니다. 매일 23:55~00:00(한국시간)에는 이용할 수 없습니다.';

export function clearInterviewProgress(userNum, storage = localStorage) {
    try { storage.removeItem(`${PREFIX}${userNum}`); } catch { /* 브라우저 저장소가 차단된 경우 */ }
}

export function readInterviewProgress(userNum, now = Date.now(), storage = localStorage) {
    try {
        const value = JSON.parse(storage.getItem(`${PREFIX}${userNum}`));
        if (!value || value.version !== 1 || !Number.isSafeInteger(value.historyNum)
            || value.historyNum <= 0 || !Number.isFinite(value.expiresAt) || value.expiresAt <= now
            || !Number.isInteger(value.questionOrder) || value.questionOrder < 1 || value.questionOrder > 5
            || typeof value.answer !== 'string' || value.answer.length > 1000) {
            clearInterviewProgress(userNum, storage);
            return null;
        }
        return value;
    } catch {
        clearInterviewProgress(userNum, storage);
        return null;
    }
}

export function saveInterviewProgress(userNum, progress, storage = localStorage) {
    try {
        storage.setItem(`${PREFIX}${userNum}`, JSON.stringify({ version: 1, ...progress }));
        return true;
    } catch { return false; }
}

export function getResumeDraft(progress, session) {
    const currentOrder = session.answers.length + 1;
    if (session.answerLocked) return { answer: session.pendingAnswer ?? '', answerLocked: true };
    if (progress?.historyNum === session.historyNum && progress.questionOrder === currentOrder) {
        return { answer: progress.answer, answerLocked: progress.answerLocked === true };
    }
    // 이전 요청이 서버에서 이미 처리됐다면 이전 문항의 초안을 재사용하지 않는다.
    return { answer: '', answerLocked: false };
}

export function getClientDeadline(response, now = Date.now()) {
    return now + Math.max(0, response.questionDeadline - response.serverTime);
}

export function remainingAnswerSeconds(deadline, now = Date.now()) {
    return Math.max(0, Math.ceil((deadline - now) / 1000));
}
