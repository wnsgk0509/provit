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
                            <h5 className="modal-title fw-bold">🚀 스터디 개설하기</h5>
                            <button type="button" className="btn-close" onClick={onClose}></button>
                        </div>
                        <div className="modal-body">
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
                                <div className="mb-3">
                                    <label className="form-label fw-semibold text-secondary small">스터디 소개 및 목표</label>
                                    <textarea 
                                        className="form-control" 
                                        rows="4" 
                                        placeholder="스터디 진행 방식, 목표, 우대 사항 등을 적어주세요."
                                        value={studyExplain}
                                        onChange={(e) => setStudyExplain(e.target.value)}
                                        style={{ resize: 'none' }}
                                    ></textarea>
                                </div>
                            </form>
                        </div>
                        <div className="modal-footer border-top-0 pt-0 justify-content-between">
                            <button type="button" className="btn btn-light rounded-pill px-4" onClick={onClose}>취소</button>
                            <button type="submit" form="studyForm" className="btn btn-primary rounded-pill px-4">개설 완료</button>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
}

export default StudyCreateModal;
