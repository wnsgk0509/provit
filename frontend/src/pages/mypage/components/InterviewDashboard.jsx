import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ChevronRight, History, RefreshCw } from 'lucide-react';
import { getInterviewResults } from '../../../api/interviewApi';
import { getDocumentReviewHistory } from '../../../api/documentReviewApi';
import { REVIEW_MODE_OPTIONS, reviewErrorMessage } from '../../documentReview/documentReviewConfig';
import InterviewRadar from './InterviewRadar';
import InterviewRecordDialog from './InterviewRecordDialog';
import { formatInterviewDate, formatScore, sortInterviewResults } from './interviewScores';
import './InterviewDashboard.css';

function InterviewDashboard({ children }) {
    const [loadCount, setLoadCount] = useState(0);
    const [state, setState] = useState({ loading: true, results: [], error: '' });
    const [selectedHistoryNum, setSelectedHistoryNum] = useState(null);

    useEffect(() => {
        let active = true;
        getInterviewResults().then((results) => {
            if (!Array.isArray(results)) throw new Error('면접 기록 응답이 올바르지 않습니다.');
            if (active) setState({ loading: false, results: sortInterviewResults(results), error: '' });
        }).catch(() => {
            if (active) setState({ loading: false, results: [], error: '면접 기록을 불러오지 못했습니다. 다시 시도해 주세요.' });
        });
        return () => { active = false; };
    }, [loadCount]);

    const reload = () => {
        setState({ loading: true, results: [], error: '' });
        setLoadCount((count) => count + 1);
    };
    const { loading, results, error } = state;
    const latest = results[0];
    const first = results.at(-1);
    const difference = latest && first ? Number(latest.totalScore) - Number(first.totalScore) : 0;

    return (
        <>
            <div className="mypage-content-row">
                <section className="mypage-interviews" aria-labelledby="interview-dashboard-title">
                    <div className="mypage-interview-heading">
                        <div className="mypage-section-title">
                            <span>INTERVIEW JOURNEY</span>
                            <h2 id="interview-dashboard-title">나의 면접 성장 기록</h2>
                            <p>최초 면접과 가장 최근 면접의 역량을 비교해 보세요.</p>
                        </div>
                        <Link className="mypage-interview-start" to="/interview">면접 연습하기 <ChevronRight size={16} /></Link>
                    </div>

                    {(loading || error || results.length === 0) && (
                        <InterviewState loading={loading} error={error} reload={reload} />
                    )}
                    {!loading && !error && results.length > 0 && (
                        <div className="mypage-interview-comparison">
                            <InterviewRadar first={first} latest={latest} />
                            <div className="mypage-interview-summary">
                                <div className="mypage-interview-latest-card">
                                    <span>가장 최근 면접</span>
                                    <div><strong>{formatScore(latest.totalScore)}</strong><span>/ 100점</span></div>
                                    <p>{formatInterviewDate(latest.interviewDate)}</p>
                                </div>
                                <dl className="mypage-interview-summary-list">
                                    <div><dt>최초 면접 점수</dt><dd>{formatScore(first.totalScore)}점</dd></div>
                                    <div><dt>최초 대비 변화</dt><dd>{results.length === 1 ? '-' : `${difference > 0 ? '+' : ''}${formatScore(difference)}점`}</dd></div>
                                    <div><dt>완료한 면접</dt><dd>{results.length}회</dd></div>
                                </dl>
                                <p className="mypage-interview-note">{results.length === 1
                                    ? '아직 기록이 1개라 최초와 최근 점수가 같습니다. 다음 면접을 완료하면 변화를 비교할 수 있어요.'
                                    : `최초 면접 ${formatInterviewDate(first.interviewDate)}부터의 점수 변화입니다.`}</p>
                            </div>
                        </div>
                    )}
                </section>

                <div className="mypage-activity-column">
                    <ActivityHistory interviewState={state} onReloadInterviews={reload} onSelectInterview={setSelectedHistoryNum} />
                    {children}
                </div>
            </div>
            {selectedHistoryNum !== null && <InterviewRecordDialog key={selectedHistoryNum} historyNum={selectedHistoryNum} onClose={() => setSelectedHistoryNum(null)} />}
        </>
    );
}

const REVIEW_PAGE_SIZE = 20;
const RECORD_TYPES = [
    { value: 'interview', label: '면접 기록' },
    { value: 'review', label: '첨삭 기록' },
];

function ActivityHistory({ interviewState, onReloadInterviews, onSelectInterview }) {
    const [recordType, setRecordType] = useState('interview');
    const [reviewState, setReviewState] = useState(null);
    const [offset, setOffset] = useState(0);
    const [revision, setRevision] = useState(0);
    const requestKey = `${offset}:${revision}`;

    useEffect(() => {
        if (recordType !== 'review') return undefined;
        let active = true;
        getDocumentReviewHistory(offset, REVIEW_PAGE_SIZE).then((records) => {
            if (!Array.isArray(records)) throw new Error('첨삭 기록 응답이 올바르지 않습니다.');
            if (active) setReviewState((previous) => ({
                key: requestKey,
                records: offset === 0 ? records : [...(previous?.records || []), ...records],
                hasMore: records.length === REVIEW_PAGE_SIZE,
                error: '',
            }));
        }).catch((error) => {
            if (active) setReviewState((previous) => ({
                key: requestKey,
                records: previous?.records || [],
                hasMore: previous?.hasMore || false,
                error: reviewErrorMessage(error, '첨삭 기록을 불러오지 못했습니다.'),
            }));
        });
        return () => { active = false; };
    }, [recordType, offset, requestKey]);

    const changeRecordType = (event) => {
        const nextType = event.target.value;
        setRecordType(nextType);
        if (nextType === 'review') {
            setReviewState(null);
            setOffset(0);
            setRevision((value) => value + 1);
        }
    };
    const isInterview = recordType === 'interview';
    const reviews = reviewState?.records || [];
    const reviewLoading = reviewState?.key !== requestKey;
    const loading = isInterview ? interviewState.loading : reviewLoading;
    const error = isInterview ? interviewState.error : reviewLoading ? '' : reviewState.error;
    const results = interviewState.results;
    const label = isInterview ? '면접 기록' : '첨삭 기록';

    return (
        <section className="mypage-interview-history" aria-labelledby="activity-history-title">
            <div className="mypage-interview-history-heading">
                <div className="mypage-section-title">
                    <span>ACTIVITY RECORDS</span>
                    <h2 id="activity-history-title"><History size={20} aria-hidden="true" /> 면접·첨삭 기록</h2>
                </div>
                {!loading && !error && <span>{isInterview ? `총 ${results.length}건` : `조회 ${reviews.length}건`}</span>}
            </div>
            <fieldset className="mypage-history-types">
                <legend className="mypage-sr-only">조회할 기록 종류</legend>
                {RECORD_TYPES.map((type) => (
                    <label key={type.value} className={recordType === type.value ? 'is-selected' : ''}>
                        <input type="radio" name="activityRecordType" value={type.value}
                            checked={recordType === type.value} onChange={changeRecordType} />
                        <span>{type.label}</span>
                    </label>
                ))}
            </fieldset>
            <div className="mypage-interview-history-list" key={recordType} role="region" aria-label={`${label} 목록`}
                aria-busy={loading} tabIndex={0}>
                {isInterview ? (
                    loading || error || results.length === 0
                        ? <InterviewState loading={loading} error={error} reload={onReloadInterviews} />
                        : results.map((result, index) => (
                            <button type="button" className="mypage-interview-history-item" key={result.historyNum}
                                onClick={() => onSelectInterview(result.historyNum)} aria-haspopup="dialog">
                                <div className="mypage-interview-history-info">
                                    <strong>{results.length - index}번째 면접
                                        {index === 0 && <span className="mypage-interview-badge">최근</span>}
                                        {index === results.length - 1 && <span className="mypage-interview-badge is-first">최초</span>}
                                    </strong>
                                    <span>{formatInterviewDate(result.interviewDate)}</span>
                                </div>
                                <span className="mypage-interview-history-score">{formatScore(result.totalScore)}<small>점</small></span>
                                <span className="mypage-interview-history-detail">상세 보기 <ChevronRight size={17} /></span>
                            </button>
                        ))
                ) : (
                    <>
                        {reviews.map((review) => (
                            <Link className="mypage-interview-history-item mypage-review-history-item" key={review.reviewNum}
                                to={`/document-review?reviewNum=${review.reviewNum}`}>
                                <div className="mypage-interview-history-info">
                                    <strong className="mypage-review-history-title" title={review.reviewTitle}>{review.reviewTitle || '첨삭 기록'}</strong>
                                    <span>{formatInterviewDate(review.createdAt)} · {REVIEW_MODE_OPTIONS.find((mode) => mode.value === review.reviewMode)?.label || '첨삭 기준'}</span>
                                    {review.resultSource === 'DUMMY' && <span>더미 결과</span>}
                                </div>
                                <span className="mypage-interview-badge">{review.reviewStatus === 'COMPLETED' ? '저장 완료' : review.reviewStatus === 'FAILED' ? '실패' : '처리 중'}</span>
                                <span className="mypage-interview-history-detail">상세 보기 <ChevronRight size={17} /></span>
                            </Link>
                        ))}
                        {reviewLoading && reviews.length === 0 && (
                            <div className="mypage-interview-state" role="status">첨삭 기록을 불러오고 있습니다.</div>
                        )}
                        {error && (
                            <div className="mypage-interview-state is-error" role="alert">
                                <p>{error}</p>
                                <button type="button" onClick={() => setRevision((value) => value + 1)}>
                                    <RefreshCw size={16} aria-hidden="true" /> 다시 불러오기
                                </button>
                            </div>
                        )}
                        {!reviewLoading && !error && reviews.length === 0 && (
                            <div className="mypage-interview-state">
                                <History size={30} aria-hidden="true" />
                                <strong>저장된 첨삭 기록이 없습니다.</strong>
                                <p>첨삭 결과를 저장하면 이곳에서 다시 확인할 수 있습니다.</p>
                                <Link to="/document-review" className="mypage-interview-start">서류 첨삭하기 <ChevronRight size={16} /></Link>
                            </div>
                        )}
                        {reviewState?.hasMore && reviews.length > 0 && !error && (
                            <button type="button" className="mypage-history-load-more" disabled={reviewLoading}
                                onClick={() => setOffset((value) => value + REVIEW_PAGE_SIZE)}>
                                {reviewLoading ? '첨삭 기록을 더 불러오는 중...' : '첨삭 기록 더 불러오기'}
                            </button>
                        )}
                    </>
                )}
            </div>
        </section>
    );
}

function InterviewState({ loading, error, reload }) {
    if (loading) return <div className="mypage-interview-state" role="status">면접 기록을 불러오고 있습니다.</div>;
    if (error) return (
        <div className="mypage-interview-state is-error" role="alert">
            <p>{error}</p><button type="button" onClick={reload}><RefreshCw size={16} /> 다시 불러오기</button>
        </div>
    );
    return (
        <div className="mypage-interview-state">
            <History size={30} aria-hidden="true" />
            <strong>첫 면접 기록을 만들어 보세요</strong>
            <p>면접을 완료하면 다섯 가지 역량 점수와 질문·답변 기록이 여기에 표시됩니다.</p>
            <Link to="/interview" className="mypage-interview-start">첫 면접 시작하기 <ChevronRight size={16} /></Link>
        </div>
    );
}

export default InterviewDashboard;
