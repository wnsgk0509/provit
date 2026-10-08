import { useEffect, useState } from 'react';
import { getDocumentReviewHistory } from '../../../api/documentReviewApi';
import { REVIEW_MODE_OPTIONS, reviewErrorMessage } from '../documentReviewConfig';

const PAGE_SIZE = 20;

function ReviewHistory({ currentReviewNum, onOpen, disabled }) {
    const [page, setPage] = useState(0);
    const [revision, setRevision] = useState(0);
    const [resource, setResource] = useState(null);
    const key = `${page}:${revision}`;
    useEffect(() => {
        let active = true;
        getDocumentReviewHistory(page * PAGE_SIZE, PAGE_SIZE).then(
            (data) => { if (active) setResource({ key, data }); },
            (error) => {
                if (active) setResource({ key, error: reviewErrorMessage(error, '첨삭 기록을 불러오지 못했습니다.') });
            },
        );
        return () => { active = false; };
    }, [page, key]);
    const current = resource?.key === key ? resource : null;
    return (
        <section className="review-history" aria-labelledby="review-history-title" aria-busy={!current}>
            <div className="review-card-heading">
                <div>
                    <h2 id="review-history-title">저장된 첨삭 기록</h2>
                    <p>서류와 요청 설정을 함께 저장한 결과를 다시 확인하세요.</p>
                </div>
                <button type="button" className="review-secondary-button" disabled={!current || disabled}
                    onClick={() => setRevision((value) => value + 1)}>새로고침</button>
            </div>
            {!current ? <p role="status">첨삭 기록을 불러오는 중입니다.</p>
                : current.error ? <p className="review-field-error" role="alert">{current.error}</p>
                : current.data.length === 0 ? <p>저장된 첨삭 기록이 없습니다.</p>
                : (
                    <ul className="review-history-list">
                        {current.data.map((review) => (
                            <li key={review.reviewNum}>
                                <button type="button" disabled={disabled}
                                    aria-current={String(review.reviewNum) === currentReviewNum ? 'true' : undefined}
                                    onClick={() => onOpen(review.reviewNum)}>
                                    <strong>{review.reviewTitle}</strong>
                                    <span>{REVIEW_MODE_OPTIONS.find((mode) => mode.value === review.reviewMode)?.label} · {review.createdAt}</span>
                                    <span>{review.reviewStatus === 'COMPLETED' ? '저장 완료' : review.reviewStatus === 'FAILED' ? '실패' : '처리 중'}
                                        {review.resultSource === 'DUMMY' ? ' · 더미 결과' : ''}</span>
                                </button>
                            </li>
                        ))}
                    </ul>
                )}
            <div className="review-stage-actions">
                <button type="button" className="review-secondary-button" disabled={page === 0 || !current || disabled}
                    onClick={() => setPage((value) => value - 1)}>이전 기록</button>
                <span>{page + 1}페이지</span>
                <button type="button" className="review-secondary-button"
                    disabled={!current || Boolean(current.error) || current.data?.length !== PAGE_SIZE || disabled}
                    onClick={() => setPage((value) => value + 1)}>다음 기록</button>
            </div>
        </section>
    );
}

export default ReviewHistory;
