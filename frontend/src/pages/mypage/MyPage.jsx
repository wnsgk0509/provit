import { useEffect, useState } from 'react';
import { KeyRound, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import './MyPage.css';

function MyPage() {
    const { user } = useAuth();
    const userEmail = user?.userEmail || user?.email || '';
    const userName = user?.userName || user?.name || '';
    const userNickname = user?.userNickname || userName;
    const [displayNickname, setDisplayNickname] = useState(userNickname);
    const [nickname, setNickname] = useState(userNickname);
    const [isNicknameEditing, setIsNicknameEditing] = useState(false);
    const [isPasswordEditing, setIsPasswordEditing] = useState(false);
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [newPasswordConfirm, setNewPasswordConfirm] = useState('');

    useEffect(() => {
        setDisplayNickname(userNickname);
        setNickname(userNickname);
    }, [userNickname]);

    const isPasswordMismatch = Boolean(newPasswordConfirm) && newPassword !== newPasswordConfirm;

    const handleCancel = () => {
        setNickname(displayNickname);
        setCurrentPassword('');
        setNewPassword('');
        setNewPasswordConfirm('');
        setIsNicknameEditing(false);
        setIsPasswordEditing(false);
    };

    const handleSubmit = (event) => {
        event.preventDefault();
        if (isPasswordMismatch) return;

        setDisplayNickname(nickname.trim() || displayNickname);
        setCurrentPassword('');
        setNewPassword('');
        setNewPasswordConfirm('');
        setIsNicknameEditing(false);
        setIsPasswordEditing(false);
        // API 연동 전까지 변경 내용은 현재 화면 상태에만 반영합니다.
    };

    return (
        <div className="mypage-page">
            <header className="mypage-intro">
                <span>MY PAGE</span>
                <h1>마이페이지</h1>
                <p>자소서 첨삭과 계정 정보를 관리하세요.</p>
            </header>

            <section className="mypage-cover-letter" aria-labelledby="cover-letter-title">
                <div className="mypage-section-title">
                    <span>COVER LETTER</span>
                    <h2 id="cover-letter-title">자소서 첨삭</h2>
                    <p>작성한 자기소개서와 첨삭 결과가 이곳에 표시됩니다.</p>
                </div>
                <div className="mypage-cover-letter-placeholder" aria-label="자소서 첨삭 내용 영역">
                    <p>자소서 첨삭 영역</p>
                </div>
            </section>

            <form className="mypage-edit-form" onSubmit={handleSubmit}>
                <section aria-labelledby="profile-edit-title">
                    <div className="mypage-edit-heading">
                        <div className="mypage-icon"><ShieldCheck size={20} /></div>
                        <div><span>PROFILE</span><h2 id="profile-edit-title">개인정보 수정</h2></div>
                    </div>

                    <dl className="mypage-info-list">
                        <div className="mypage-info-row">
                            <dt>이메일(아이디)</dt>
                            <dd><input type="email" value={userEmail} disabled aria-label="이메일 아이디" /></dd>
                        </div>
                        <div className="mypage-info-row">
                            <dt>실명</dt>
                            <dd><input type="text" value={userName} disabled aria-label="실명" /></dd>
                        </div>
                        <div className="mypage-info-row">
                            <dt>닉네임</dt>
                            <dd>
                                {isNicknameEditing ? (
                                    <input type="text" value={nickname} onChange={(event) => setNickname(event.target.value)} aria-label="닉네임" autoFocus required />
                                ) : (
                                    <input type="text" value={displayNickname} readOnly onClick={() => setIsNicknameEditing(true)} className="mypage-editable-trigger" aria-label="닉네임 변경" />
                                )}
                            </dd>
                        </div>
                        <div className="mypage-info-row">
                            <dt>비밀번호</dt>
                            <dd>
                                {!isPasswordEditing && <input type="text" value="••••••••" readOnly onClick={() => setIsPasswordEditing(true)} className="mypage-editable-trigger" aria-label="비밀번호 변경" />}
                            </dd>
                        </div>
                    </dl>
                </section>

                {isPasswordEditing && (
                    <section className="mypage-password-editor" aria-labelledby="password-edit-title">
                        <div className="mypage-password-title"><KeyRound size={17} /><h3 id="password-edit-title">비밀번호 변경</h3></div>
                        <div className="mypage-field">
                            <label htmlFor="current-password">현재 비밀번호</label>
                            <input id="current-password" type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} placeholder="현재 비밀번호를 입력하세요" autoComplete="current-password" required />
                        </div>
                        <div className="mypage-field">
                            <label htmlFor="new-password">새 비밀번호</label>
                            <input id="new-password" type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} placeholder="새 비밀번호를 입력하세요" autoComplete="new-password" required />
                        </div>
                        <div className="mypage-field">
                            <label htmlFor="new-password-confirm">새 비밀번호 확인</label>
                            <input id="new-password-confirm" type="password" value={newPasswordConfirm} onChange={(event) => setNewPasswordConfirm(event.target.value)} placeholder="새 비밀번호를 다시 입력하세요" autoComplete="new-password" aria-describedby={isPasswordMismatch ? 'password-mismatch' : undefined} required />
                            {isPasswordMismatch && <p id="password-mismatch" className="mypage-error">새 비밀번호가 일치하지 않습니다.</p>}
                        </div>
                    </section>
                )}

                <div className="mypage-actions">
                    <button type="button" className="mypage-cancel" onClick={handleCancel}>취소</button>
                    <button type="submit" className="mypage-save" disabled={isPasswordMismatch}>변경사항 저장하기</button>
                </div>

                <div className="mypage-withdrawal"><button type="button">회원 탈퇴</button></div>
            </form>
        </div>
    );
}

export default MyPage;
