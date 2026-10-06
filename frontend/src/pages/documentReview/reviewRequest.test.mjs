import test from 'node:test';
import assert from 'node:assert/strict';
import { saveReviewRequest, loadReviewRequest, resumeReviewRequest, reviewSelectionParams, createReviewRequestId } from './reviewRequest.js';

const requestId = '7d8f6541-abcd-4b4d-9b02-37e7532b9259';
const payload = { resumeNum: 11, letterNum: 12, portfolioNum: null, reviewMode: 'comprehensive', customCriteria: null, instructions: '' };
function storage() {
    const values = new Map();
    return { getItem: (key) => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: (key) => values.delete(key) };
}
const missing = () => Promise.reject({ response: { status: 404 } });

test('UUID generation also works without randomUUID on HTTP deployments', () => {
    assert.equal(createReviewRequestId({ getRandomValues: (bytes) => bytes.fill(255) }), 'ffffffff-ffff-4fff-bfff-ffffffffffff');
    assert.equal(createReviewRequestId({ randomUUID: () => requestId }), requestId);
});

test('editing document selection preserves the unresolved request ID', () => {
    const before = new URLSearchParams({ resumeNum: '11', portfolioNum: '13', requestId, type: 'resume', document: '11', reviewNum: '7' });
    const next = reviewSelectionParams(before, { resumeNum: '21', letterNum: '12', portfolioNum: '' });
    assert.equal(next.get('requestId'), requestId);
    assert.equal(next.get('resumeNum'), '21');
    for (const key of ['portfolioNum', 'reviewNum', 'type', 'document']) assert.equal(next.has(key), false);
});

test('request is persisted before sending and restored after refresh with the same ID', () => {
    const browserStorage = storage();
    const request = saveReviewRequest(payload, '', browserStorage, () => requestId);
    assert.deepEqual(loadReviewRequest(requestId, browserStorage), request);
    assert.deepEqual(saveReviewRequest(payload, requestId, browserStorage, () => assert.fail('New ID')), request);
});

test('storage failure prevents starting a request whose ID cannot be restored', () => {
    assert.throws(() => saveReviewRequest(payload, '', { getItem: () => null, setItem: () => { throw new Error('blocked'); } }, () => requestId), /보관하지 못했습니다/);
});

test('changing an unresolved request preserves the original payload', () => {
    const browserStorage = storage();
    const original = saveReviewRequest(payload, requestId, browserStorage);
    assert.throws(() => saveReviewRequest({ ...payload, instructions: '다른 요청' }, requestId, browserStorage), /기존 요청/);
    assert.deepEqual(loadReviewRequest(requestId, browserStorage), original);
});

test('processing, completed and failed records are recovered without another POST', async () => {
    for (const reviewStatus of ['PROCESSING', 'COMPLETED', 'FAILED']) {
        const record = { reviewNum: 1, reviewStatus };
        assert.equal(await resumeReviewRequest(requestId, storage(), async () => record, () => assert.fail('Duplicate POST')), record);
    }
});

test('only a missing server record resends the exact stored request ID and payload', async () => {
    const browserStorage = storage();
    const request = saveReviewRequest(payload, requestId, browserStorage);
    const result = await resumeReviewRequest(requestId, browserStorage, missing, async (retry) => {
        assert.deepEqual(retry, request);
        return { reviewNum: 2 };
    });
    assert.equal(result.reviewNum, 2);
});

test('authentication, timeout and server errors never trigger another POST', async () => {
    for (const status of [401, 403, 500, undefined]) {
        const error = { response: { status } };
        await assert.rejects(resumeReviewRequest(requestId, storage(), async () => { throw error; }, () => assert.fail('Unsafe POST')), (value) => value === error);
    }
});

test('missing or corrupt recovery payload is not submitted', async () => {
    await assert.rejects(resumeReviewRequest(requestId, storage(), missing, () => assert.fail('Missing payload')), /재전송할 요청 정보/);
    assert.equal(loadReviewRequest(requestId, { getItem: () => '{broken' }), null);
    assert.equal(loadReviewRequest(requestId, { getItem: () => JSON.stringify({ ...payload, requestId: 'another' }) }), null);
});
