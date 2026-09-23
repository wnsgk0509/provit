import React, { useState } from 'react';
import { createStudy } from '../../api/studyApi';
import { useAuth } from '../../context/AuthContext';

function StudyCreateModal({ show, onClose, onSuccess }) {
    const { user } = useAuth();
    const [studyName, setStudyName] = useState('');
    const [studyExplain, setStudyExplain] = useState('');

    if (!show) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!user) {
            alert('로그인이 필요합니다.');
            return;
        }
        if (!studyName.trim() || !studyExplain.trim()) {
            alert('스터디 이름과 소개를 모두 입력해주세요.');
            return;
        }

        try {
            const result = await createStudy({
                userNum: user.userNum,
                studyName,
                studyExplain
            });
            
            if (result && result.responseCode && result.responseCode.code === 200) {
                alert('스터디가 성공적으로 개설되었습니다!');
                setStudyName('');
                setStudyExplain('');
                onSuccess(); // 목록 새로고침
                onClose(); // 모달 닫기
            } else {
                alert('스터디 개설에 실패했습니다.');
            }
        } catch (error) {
            alert('서버 오류가 발생했습니다.');
        }
    };

    return (
        <>
            <div className="modal show d-block" tabIndex="-1" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}>
                <div className="modal-dialog modal-dialog-centered">
                    <div className="modal-content border-0 shadow-lg" style={{ borderRadius: '1rem' }}>
                        <div className="modal-header border-bottom-0 pb-0">
                            <h5 className="modal-title fw-bold">스터디 만들기</h5>
                            <button type="button" className="btn-close" onClick={onClose}></button>
                        </div>
                        <div className="modal-body p-4">
                            <form id="studyForm" onSubmit={handleSubmit}>
                                <div className="mb-3">
                                    <label className="form-label fw-semibold text-secondary small">스터디 이름</label>
                                    <input 
                                        type="text" 
                                        className="form-control" 
                                        placeholder="예) 프론트엔드 모의 면접 스터디"
                                        value={studyName}
                                        onChange={(e) => setStudyName(e.target.value)}
                                        maxLength={100}
                                    />
                                </div>
                                <div className="mb-4">
                                    <label className="form-label fw-bold small">운영 방식</label>
                                    <textarea 
                                        className="form-control" 
                                        rows="5" 
                                        placeholder="예) 매주 수요일 20:00 · 온라인 음성 · 최대 6명&#13;&#10;&#13;&#10;스터디 소개..."
                                        value={studyExplain}
                                        onChange={(e) => setStudyExplain(e.target.value)}
                                        style={{ resize: 'none' }}
                                    ></textarea>
                                </div>
                                <button type="submit" className="btn btn-primary w-100 py-3 fw-bold rounded-3">
                                    스터디 개설
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}

export default StudyCreateModal;
