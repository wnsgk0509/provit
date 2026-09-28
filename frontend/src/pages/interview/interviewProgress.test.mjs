import assert from 'node:assert/strict';
import { test } from 'node:test';
import { clearInterviewProgress, getClientDeadline, getResumeDraft,
    readInterviewProgress, remainingAnswerSeconds, saveInterviewProgress } from './interviewProgress.js';

const storage = () => {
    const values = new Map();
    return { getItem: (key) => values.get(key) ?? null,
        setItem: (key, value) => values.set(key, value), removeItem: (key) => values.delete(key) };
};
const progress = { historyNum: 17, expiresAt: 10000, questionOrder: 2, answer: '작성 중 답변', answerLocked: false };

test('작성 중 답변과 진행 ID를 재접속 시 복원하고 회원별로 분리한다', () => {
    const saved = storage();
    assert.equal(saveInterviewProgress(7, progress, saved), true);
    assert.equal(readInterviewProgress(7, 9999, saved).answer, progress.answer);
    assert.equal(readInterviewProgress(8, 9999, saved), null);
    clearInterviewProgress(8, saved);
    assert.equal(readInterviewProgress(7, 9999, saved).historyNum, 17);
});

test('만료 시각부터 저장 데이터를 제거하고 손상된 데이터도 복구 대상에서 제외한다', () => {
    const saved = storage();
    saveInterviewProgress(7, progress, saved);
    assert.equal(readInterviewProgress(7, 10000, saved), null);
    assert.equal(saved.getItem('provit:interview-progress:7'), null);
    saved.setItem('provit:interview-progress:7', '{broken');
    assert.equal(readInterviewProgress(7, 0, saved), null);
    saved.setItem('provit:interview-progress:7', JSON.stringify({ version: 1, ...progress, answer: 123 }));
    assert.equal(readInterviewProgress(7, 0, saved), null);
});

test('응답을 받지 못한 제출이 서버에서 성공했으면 이전 초안을 다음 문항에 붙이지 않는다', () => {
    const session = { historyNum: 17, answers: [{}, {}] };
    assert.deepEqual(getResumeDraft(progress, session), { answer: '', answerLocked: false });
    assert.deepEqual(getResumeDraft(progress, { ...session, answers: [{}] }),
        { answer: progress.answer, answerLocked: false });
});

test('평가 후 저장 재시도에서는 서버가 확정한 답변을 잠금 상태로 복원한다', () => {
    const session = { historyNum: 17, answers: [{}, {}, {}, {}], answerLocked: true, pendingAnswer: '확정된 답변' };
    assert.deepEqual(getResumeDraft(progress, session), { answer: '확정된 답변', answerLocked: true });
});

test('재개 시 서버의 남은 시간을 사용하고 이미 끝난 시간을 다시 부여하지 않는다', () => {
    const deadline = getClientDeadline({ serverTime: 1000, questionDeadline: 3500 }, 10000);
    assert.equal(deadline, 12500);
    assert.equal(remainingAnswerSeconds(deadline, 10000), 3);
    assert.equal(remainingAnswerSeconds(deadline, 12500), 0);
    assert.equal(getClientDeadline({ serverTime: 4000, questionDeadline: 3500 }, 10000), 10000);
});

test('브라우저 저장소 차단으로 면접 자체가 예외로 중단되지 않는다', () => {
    const denied = { getItem: () => { throw new Error('denied'); },
        setItem: () => { throw new Error('denied'); }, removeItem: () => { throw new Error('denied'); } };
    assert.equal(saveInterviewProgress(7, progress, denied), false);
    assert.equal(readInterviewProgress(7, 0, denied), null);
});
