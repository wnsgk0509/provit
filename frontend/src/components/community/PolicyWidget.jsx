import React, { useState } from 'react';

const PolicyWidget = () => {
    const [showModal, setShowModal] = useState(false);

    return (
        <>
            <div className="card shadow-sm border-0 rounded-4 p-4">
                <h6 className="fw-bold mb-3">커뮤니티 운영 안내</h6>
                <p className="text-muted small mb-4" style={{ lineHeight: '1.6' }}>
                    채용 사기, 개인정보 노출, 비방 글은 관리자 검토 후 숨김 또는 삭제될 수 있습니다.
                </p>
                <button 
                    className="btn btn-outline-secondary btn-sm w-100 rounded-pill py-2 fw-semibold"
                    onClick={() => setShowModal(true)}
                >
                    운영 정책 보기
                </button>
            </div>

            {/* 운영 정책 모달 */}
            {showModal && (
                <div className="modal fade show d-block" style={{ backgroundColor: 'rgba(0,0,0,0.5)' }} tabIndex="-1">
                    <div className="modal-dialog modal-dialog-centered modal-dialog-scrollable">
                        <div className="modal-content rounded-4 border-0 shadow">
                            <div className="modal-header border-bottom-0 pb-0">
                                <h5 className="modal-title fw-bold px-2 pt-2">커뮤니티 운영 정책</h5>
                                <button type="button" className="btn-close" onClick={() => setShowModal(false)}></button>
                            </div>
                            <div className="modal-body px-4 py-4">
                                <div className="text-muted small" style={{ lineHeight: '1.8' }}>
                                    <h6 className="fw-bold text-dark mt-2 mb-2">1. 기본 원칙</h6>
                                    <p>본 커뮤니티는 취업 준비생과 현직자가 함께 정보를 나누고 소통하는 공간입니다. 서로를 존중하며 건전한 활동을 부탁드립니다.</p>
                                    
                                    <h6 className="fw-bold text-dark mt-4 mb-2">2. 게시글 및 댓글 작성</h6>
                                    <ul>
                                        <li>채용 관련 정보, 면접 후기, 자소서 팁 등을 자유롭게 나눌 수 있습니다.</li>
                                        <li>욕설, 비방, 차별적 발언 및 분란을 조장하는 글은 통보 없이 삭제될 수 있습니다.</li>
                                        <li>거짓 정보나 불법 홍보성 글 작성 시 계정 이용이 제한될 수 있습니다.</li>
                                    </ul>
                                    
                                    <h6 className="fw-bold text-dark mt-4 mb-2">3. 개인정보 보호</h6>
                                    <p>타인의 개인정보(연락처, 이메일, 실명 등)를 무단으로 게시하는 행위는 엄격히 금지됩니다. 자신의 개인정보 또한 노출되지 않도록 주의 바랍니다.</p>
                                </div>
                            </div>
                            <div className="modal-footer border-top-0 pt-0 pb-3">
                                <button type="button" className="btn btn-secondary rounded-pill px-4" onClick={() => setShowModal(false)}>닫기</button>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
};

export default PolicyWidget;
