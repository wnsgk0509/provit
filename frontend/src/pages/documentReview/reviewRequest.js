const STORAGE_PREFIX = 'provit:document-review-request:';
const REQUEST_ID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function createReviewRequestId(cryptoProvider = globalThis.crypto) {
    if (typeof cryptoProvider.randomUUID === 'function') return cryptoProvider.randomUUID();
    const bytes = cryptoProvider.getRandomValues(new Uint8Array(16));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    const hex = Array.from(bytes, (value) => value.toString(16).padStart(2, '0')).join('');
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

export function reviewSelectionParams(params, selectedIds) {
    const next = new URLSearchParams(params);
    for (const key of ['resumeNum', 'letterNum', 'portfolioNum']) {
        if (selectedIds[key]) next.set(key, selectedIds[key]);
        else next.delete(key);
    }
    for (const key of ['type', 'document', 'reviewNum']) next.delete(key);
    return next;
}

export function saveReviewRequest(payload, requestId, storage, createId) {
    const request = { ...payload, requestId: requestId || createId() };
    if (!REQUEST_ID_PATTERN.test(request.requestId)) throw new Error('첨삭 요청 ID가 올바르지 않습니다.');
    const key = STORAGE_PREFIX + request.requestId;
    const serialized = JSON.stringify(request);
    try {
        const previous = storage.getItem(key);
        if (previous && previous !== serialized) {
            throw new Error('기존 요청의 결과를 먼저 확인해 주세요. 처리 여부가 확인되기 전에는 첨삭 조건을 변경할 수 없습니다.');
        }
        storage.setItem(key, serialized);
        if (storage.getItem(key) !== serialized) throw new Error('Request was not saved');
    } catch (error) {
        if (error.message.startsWith('기존 요청')) throw error;
        throw new Error('요청 정보를 브라우저에 보관하지 못했습니다. 브라우저의 저장소 사용을 허용한 후 다시 시도해 주세요.', { cause: error });
    }
    return request;
}

export function loadReviewRequest(requestId, storage) {
    try {
        const request = JSON.parse(storage.getItem(STORAGE_PREFIX + requestId));
        if (!REQUEST_ID_PATTERN.test(requestId) || request?.requestId !== requestId) return null;
        if (!Number.isSafeInteger(request.resumeNum) || request.resumeNum < 1 ||
            !Number.isSafeInteger(request.letterNum) || request.letterNum < 1) return null;
        return request;
    } catch {
        return null;
    }
}

export function removeReviewRequest(requestId, storage) {
    try { storage.removeItem(STORAGE_PREFIX + requestId); } catch { return; }
}

export async function resumeReviewRequest(requestId, storage, findRequest, submitRequest) {
    try {
        return await findRequest(requestId);
    } catch (error) {
        if (error.response?.status !== 404) throw error;
        const request = loadReviewRequest(requestId, storage);
        if (!request) throw new Error('이 브라우저에 재전송할 요청 정보가 없습니다. 서류를 다시 선택해 같은 요청 ID로 확인해 주세요.', { cause: error });
        return submitRequest(request);
    }
}
