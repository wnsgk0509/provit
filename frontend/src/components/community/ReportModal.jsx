import React, { useState } from 'react';
import { submitReport } from '../../api/communityApi';

const REASONS = [
    { value: 'SPAM', label: '스팸/홍보성' },
    { value: 'ABUSE', label: '욕설/비방' },
    { value: 'ADULT', label: '음란물' },
    { value: 'ILLEGAL', label: '불법 정보' },
    { value: 'OTHER', label: '기타' }
];

function ReportModal({ show, onClose, targetType, targetNum }) {
    const [reason, setReason] = useState('SPAM');
    const [loading, setLoading] = useState(false);

    if (!show) return null;

    const handleSubmit = async () => {
        setLoading(true);
        try {
            const result = await submitReport({
                targetType,
                targetNum,
                reportReason: reason
            });
            if (result && result.responseCode && result.responseCode.code === 200) {
                alert('신고가 정상적으로 접수되었습니다.');
                onClose();
            } else {
                // 백엔드에서 에러 발생 시 data(예외 메시지) 또는 responseCode.message를 보여줌
                alert(result.data || result.responseCode?.message || '신고 처리에 실패했습니다.');
            }
        } catch (error) {
            alert(error.response?.data?.data || error.response?.data?.responseCode?.message || '신고 접수 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            {/* Backdrop */}
            <div className="modal-backdrop fade show"></div>
            {/* Modal */}
            <div className="modal fade show d-block" tabIndex="-1" role="dialog" aria-modal="true">
                <div className="modal-dialog modal-dialog-centered">
                    <div className="modal-content shadow-lg border-0 rounded-4">
                        <div className="modal-header bg-light border-bottom-0 rounded-top-4">
                            <h5 className="modal-title fw-bold text-danger">
                                <i className="bi bi-exclamation-triangle-fill me-2"></i>신고하기
                            </h5>
                            <button type="button" className="btn-close" onClick={onClose} aria-label="Close"></button>
                        </div>
                        <div className="modal-body p-4">
                            <p className="text-secondary mb-4">
                                부적절한 내용이 포함된 {targetType === 'POST' ? '게시글' : '댓글'}인가요?<br/>
                                신고 사유를 선택해 주시면 관리자가 확인 후 조치하겠습니다.
                            </p>
                            <div className="d-flex flex-column gap-3">
                                {REASONS.map(r => (
                                    <div className="form-check custom-radio" key={r.value}>
                                        <input 
                                            className="form-check-input" 
                                            type="radio" 
                                            name="reportReason" 
                                            id={`reason_${r.value}`}
                                            value={r.value}
                                            checked={reason === r.value}
                                            onChange={(e) => setReason(e.target.value)}
                                        />
                                        <label className="form-check-label fw-medium ms-2" htmlFor={`reason_${r.value}`}>
                                            {r.label}
                                        </label>
                                    </div>
                                ))}
                            </div>
                        </div>
                        <div className="modal-footer border-top-0 pt-0 pb-4 px-4">
                            <button type="button" className="btn btn-light w-100 mb-2 fw-medium" onClick={onClose}>
                                취소
                            </button>
                            <button type="button" className="btn btn-danger w-100 m-0 fw-bold" onClick={handleSubmit} disabled={loading}>
                                {loading ? '처리 중...' : '신고 접수'}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}

export default ReportModal;
