import { useEffect, useId, useRef, useState } from 'react';
import { RefreshCw, X } from 'lucide-react';
import { getInterviewRecord } from '../../../api/interviewApi';
import { SCORE_ITEMS, formatInterviewDate, formatScore } from './interviewScores';

const FEEDBACK_ITEMS = [
    { key: 'strengths', label: '잘한 점' },
    { key: 'weaknesses', label: '아쉬운 점' },
    { key: 'comparison', label: '이전 기록 비교' },
    { key: 'improvements', label: '개선할 점' },
];

function InterviewRecordDialog({ historyNum, onClose }) {
    const dialogRef = useRef(null);
    const titleId = useId();
    const [loadCount, setLoadCount] = useState(0);
    const [state, setState] = useState({ loading: true, record: null, error: '' });

    useEffect(() => {
        const dialog = dialogRef.current;
        dialog.showModal();
        return () => { dialog.close(); };
    }, []);

    useEffect(() => {
        let active = true;
        getInterviewRecord(historyNum).then((record) => {
            if (!record?.history || !record?.result) throw new Error('면접 기록 응답이 올바르지 않습니다.');
            if (active) setState({ loading: false, record, error: '' });
        }).catch((error) => {
            if (active) setState({ loading: false, record: null, error: error.response?.status === 404
                ? '조회할 수 있는 면접 기록이 없습니다.' : '면접 상세 기록을 불러오지 못했습니다.' });
        });
        return () => { active = false; };
    }, [historyNum, loadCount]);

    const reload = () => {
        setState({ loading: true, record: null, error: '' });
        setLoadCount((count) => count + 1);
    };
    const { loading, record, error } = state;
    return (
        <dialog className="mypage-interview-dialog" ref={dialogRef} aria-labelledby={titleId} onCancel={onClose}>
            <div className="mypage-interview-dialog-heading">
                <div><span>INTERVIEW RECORD</span><h2 id={titleId}>면접 상세 기록</h2></div>
                <button type="button" className="mypage-interview-close" onClick={onClose} aria-label="면접 상세 기록 닫기"><X size={22} /></button>
            </div>
            {loading && <div className="mypage-interview-state" role="status">질문과 답변을 불러오고 있습니다.</div>}
            {!loading && error && <div className="mypage-interview-state is-error" role="alert"><p>{error}</p><button type="button" onClick={reload}><RefreshCw size={16} /> 다시 불러오기</button></div>}
            {!loading && record && (
                <>
                    <div className="mypage-interview-record-summary">
                        <span>{formatInterviewDate(record.result.interviewDate)}</span>
                        <strong>종합 점수 {formatScore(record.result.totalScore)}<small> / 100점</small></strong>
                    </div>
                    <dl className="mypage-interview-record-scores">
                        {SCORE_ITEMS.map((item) => <div key={item.key}><dt>{item.label}</dt><dd>{formatScore(record.result[item.key])}<small>점</small></dd></div>)}
                    </dl>
                    <h3 className="mypage-interview-record-title">면접 평가</h3>
                    <div className="mypage-interview-feedback">
                        {FEEDBACK_ITEMS.map((item) => <article key={item.key}><h4>{item.label}</h4><p>{record.result[item.key] || '등록된 평가 내용이 없습니다.'}</p></article>)}
                    </div>
                    <h3 className="mypage-interview-record-title">질문과 답변</h3>
                    <ol className="mypage-interview-qa">
                        {[1, 2, 3, 4, 5].map((order) => (
                            <li key={order}>
                                <h4><span>Q{order}</span>{record.history[`question${order}`] || '저장된 질문이 없습니다.'}</h4>
                                <div><span>나의 답변</span><p>{record.history[`answer${order}`]?.trim() || '제출된 답변이 없습니다.'}</p></div>
                            </li>
                        ))}
                    </ol>
                </>
            )}
        </dialog>
    );
}

export default InterviewRecordDialog;
