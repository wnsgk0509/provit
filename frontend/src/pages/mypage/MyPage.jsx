import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ChevronRight, Eye, EyeOff, FileText, KeyRound, ShieldCheck } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import client from '../../api/client';
import { getCoverLetterList, getPortfolioList, getResumeList } from '../../api/documentApi';
import './MyPage.css';

// 회원가입과 동일한 비밀번호 규칙: 8자 이상, 영문 대/소문자·숫자·특수문자 포함
const PASSWORD_PATTERN = /^(?=\S{8,}$)(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).*$/;

const DOCUMENT_OPTIONS = [
    { value: 'resume', label: '이력서', load: getResumeList },
    { value: 'cover-letter', label: '자기소개서', load: getCoverLetterList },
    { value: 'portfolio', label: '포트폴리오', load: getPortfolioList },
];

function MyPage() {
    const { user, updateUser, logout } = useAuth();
    const navigate = useNavigate();
    const userEmail = user?.userEmail || user?.email || '';
    const userName = user?.userName || user?.name || '';
    const userNickname = user?.userNickname || userName;
    const [displayNickname, setDisplayNickname] = useState(userNickname);
    const [nickname, setNickname] = useState(userNickname);
    const [isNicknameEditing, setIsNicknameEditing] = useState(false);
    const [isPasswordEditing, setIsPasswordEditing] = useState(false);
    const [isNicknameChecking, setIsNicknameChecking] = useState(false);
    const [isNicknameAvailable, setIsNicknameAvailable] = useState(null);
    const [nicknameMessage, setNicknameMessage] = useState('');
    const [currentPassword, setCurrentPassword] = useState('');
    const [newPassword, setNewPassword] = useState('');
    const [newPasswordConfirm, setNewPasswordConfirm] = useState('');
    const [showCurrentPassword, setShowCurrentPassword] = useState(false);
    const [showNewPassword, setShowNewPassword] = useState(false);
    const [showNewPasswordConfirm, setShowNewPasswordConfirm] = useState(false);
    const [showWithdrawalPassword, setShowWithdrawalPassword] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [saveMessage, setSaveMessage] = useState('');
    const [isWithdrawalOpen, setIsWithdrawalOpen] = useState(false);
    const [withdrawalPassword, setWithdrawalPassword] = useState('');
    const [withdrawalError, setWithdrawalError] = useState('');
    const [isWithdrawing, setIsWithdrawing] = useState(false);
    const [selectedDocumentType, setSelectedDocumentType] = useState('resume');
    const [documentList, setDocumentList] = useState([]);
    const [isDocumentListLoading, setIsDocumentListLoading] = useState(true);
    const [documentListError, setDocumentListError] = useState('');

    useEffect(() => {
        setDisplayNickname(userNickname);
        setNickname(userNickname);
    }, [userNickname]);

    useEffect(() => {
        if (!isNicknameEditing) return undefined;

        const trimmedNickname = nickname.trim();
        if (!trimmedNickname) {
            setIsNicknameAvailable(false);
            setNicknameMessage('닉네임을 입력해 주세요.');
            return undefined;
        }
        if (trimmedNickname === displayNickname) {
            setIsNicknameAvailable(true);
            setNicknameMessage('현재 사용 중인 닉네임입니다.');
            return undefined;
        }

        // 입력이 멈춘 뒤에만 중복 확인 API를 호출해 불필요한 요청을 줄인다.
        const timerId = window.setTimeout(async () => {
            setIsNicknameChecking(true);
            try {
                const response = await client.get('/auth/check-nickname', { params: { nickname: trimmedNickname } });
                const available = response.data?.data?.available === true;
                setIsNicknameAvailable(available);
                setNicknameMessage(available ? '사용 가능한 닉네임입니다.' : '이미 사용 중인 닉네임입니다.');
            } catch {
                setIsNicknameAvailable(false);
                setNicknameMessage('닉네임 중복 확인에 실패했습니다.');
            } finally {
                setIsNicknameChecking(false);
            }
        }, 400);

        return () => window.clearTimeout(timerId);
    }, [nickname, isNicknameEditing, displayNickname]);

    useEffect(() => {
        let isActive = true;
        const selectedOption = DOCUMENT_OPTIONS.find(
            (option) => option.value === selectedDocumentType,
        );

        selectedOption.load()
            .then((documents) => {
                if (isActive) setDocumentList(documents || []);
            })
            .catch((error) => {
                if (!isActive) return;
                const responseData = error.response?.data;
                setDocumentListError(
                    responseData?.data
                    || responseData?.responseCode?.message
                    || '문서 목록을 불러오지 못했습니다.',
                );
            })
            .finally(() => {
                if (isActive) setIsDocumentListLoading(false);
            });

        return () => {
            isActive = false;
        };
    }, [selectedDocumentType]);

    const isPasswordMismatch = Boolean(newPasswordConfirm) && newPassword !== newPasswordConfirm;
    const isPasswordInvalid = Boolean(newPassword) && !PASSWORD_PATTERN.test(newPassword);
    const isPasswordSameAsCurrent = Boolean(currentPassword) && currentPassword === newPassword;
    const isNicknameSaveBlocked = isNicknameEditing && (!nickname.trim() || isNicknameChecking || isNicknameAvailable !== true);

    const handleDocumentTypeChange = (event) => {
        setSelectedDocumentType(event.target.value);
        setDocumentList([]);
        setDocumentListError('');
        setIsDocumentListLoading(true);
    };

    const handleCancel = () => {
        setNickname(displayNickname);
        setCurrentPassword('');
        setNewPassword('');
        setNewPasswordConfirm('');
        // 편집을 다시 열 때 비밀번호가 기본적으로 가려진 상태가 되도록 초기화한다.
        setShowCurrentPassword(false);
        setShowNewPassword(false);
        setShowNewPasswordConfirm(false);
        setIsNicknameEditing(false);
        setIsPasswordEditing(false);
        setIsNicknameAvailable(null);
        setNicknameMessage('');
        setSaveMessage('');
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (isPasswordMismatch || isPasswordInvalid || isPasswordSameAsCurrent || isNicknameSaveBlocked) return;

        const passwordChangeRequested = isPasswordEditing;

        setIsSaving(true);
        setSaveMessage('');

        try {
            // undefined 항목은 JSON 요청에서 제외되어 수정하지 않는다.
            const response = await client.patch('/auth/me', {
                userNickname: isNicknameEditing ? nickname.trim() : undefined,
                currentPassword: isPasswordEditing ? currentPassword : undefined,
                newPassword: isPasswordEditing ? newPassword : undefined,
                newPasswordConfirm: isPasswordEditing ? newPasswordConfirm : undefined,
            });
            const updatedUser = response.data?.data;

            if (!updatedUser) {
                throw new Error('수정된 회원 정보를 받지 못했습니다.');
            }

            if (passwordChangeRequested) {
                // 비밀번호 변경 뒤 서버가 기존 JWT를 무효화하므로 즉시 재로그인한다.
                logout();
                window.alert('비밀번호가 변경되었습니다. 다시 로그인해 주세요.');
                navigate('/login', { replace: true });
                return;
            }

            // 서버가 반환한 최신 사용자 정보로 전역 인증 상태를 동기화한다.
            updateUser(updatedUser);
            setDisplayNickname(updatedUser.userNickname || updatedUser.userName || '');
            setNickname(updatedUser.userNickname || updatedUser.userName || '');
            setCurrentPassword('');
            setNewPassword('');
            setNewPasswordConfirm('');
            setIsNicknameEditing(false);
            setIsPasswordEditing(false);
            setIsNicknameAvailable(null);
            setNicknameMessage('');
            setSaveMessage('회원 정보가 수정되었습니다.');
        } catch (error) {
            const responseData = error.response?.data;
            setSaveMessage(responseData?.data || responseData?.responseCode?.message || error.message || '회원 정보 수정에 실패했습니다.');
        } finally {
            setIsSaving(false);
        }
    };

    const handleWithdrawalClick = () => {
        // 첫 확인 뒤에만 현재 비밀번호 입력 영역을 열어 실수로 탈퇴하는 일을 줄인다.
        if (window.confirm('정말로 탈퇴하시겠습니까? 탈퇴 후에는 동일한 이메일로 재가입할 수 없습니다.')) {
            // 이전 탈퇴 시도에서 입력·표시된 민감 정보를 남기지 않는다.
            setWithdrawalPassword('');
            setShowWithdrawalPassword(false);
            setIsWithdrawalOpen(true);
            setWithdrawalError('');
        }
    };

    const closeWithdrawalPanel = () => {
        // 취소 후에도 비밀번호 값과 오류가 다음 시도에 남지 않도록 초기화한다.
        setWithdrawalPassword('');
        setWithdrawalError('');
        setShowWithdrawalPassword(false);
        setIsWithdrawalOpen(false);
    };

    const handleWithdrawalSubmit = async () => {
        if (!withdrawalPassword) {
            setWithdrawalError('회원 탈퇴를 위해 현재 비밀번호를 입력해 주세요.');
            return;
        }

        setIsWithdrawing(true);
        setWithdrawalError('');

        try {
            // Axios DELETE 요청 본문은 data 속성으로 전달한다.
            await client.delete('/auth/me', { data: { currentPassword: withdrawalPassword } });
            closeWithdrawalPanel();
            // 탈퇴 완료 뒤 남아 있는 인증 정보를 제거하고 로그인 화면으로 이동한다.
            logout();
            navigate('/login', { replace: true });
        } catch (error) {
            const responseData = error.response?.data;
            setWithdrawalError(responseData?.data || responseData?.responseCode?.message || '회원 탈퇴 처리에 실패했습니다.');
        } finally {
            setIsWithdrawing(false);
        }
    };

    return (
        <div className="mypage-page">
            <header className="mypage-intro">
                <span>MY PAGE</span>
                <h1>마이페이지</h1>
                <p>자소서 첨삭과 계정 정보를 관리하세요.</p>
            </header>

            <section className="mypage-documents" aria-labelledby="document-management-title">
                <div className="mypage-section-title">
                    <span>DOCUMENTS</span>
                    <h2 id="document-management-title">취업 문서 관리</h2>
                    <p>작성한 이력서, 자기소개서와 포트폴리오를 확인하세요.</p>
                </div>

                <fieldset className="mypage-document-types">
                    <legend>조회할 문서 종류</legend>
                    {DOCUMENT_OPTIONS.map((option) => (
                        <label
                            className={selectedDocumentType === option.value ? 'is-selected' : ''}
                            key={option.value}
                        >
                            <input
                                type="radio"
                                name="documentType"
                                value={option.value}
                                checked={selectedDocumentType === option.value}
                                onChange={handleDocumentTypeChange}
                            />
                            <span>{option.label}</span>
                        </label>
                    ))}
                </fieldset>

                <div className="mypage-document-list" aria-live="polite">
                    {isDocumentListLoading && (
                        <div className="mypage-document-state" role="status">
                            문서 목록을 불러오고 있습니다.
                        </div>
                    )}

                    {!isDocumentListLoading && documentListError && (
                        <div className="mypage-document-state is-error" role="alert">
                            {documentListError}
                        </div>
                    )}

                    {!isDocumentListLoading && !documentListError && documentList.length === 0 && (
                        <div className="mypage-document-state">
                            등록된 {DOCUMENT_OPTIONS.find((option) => option.value === selectedDocumentType)?.label}가 없습니다.
                        </div>
                    )}

                    {!isDocumentListLoading && !documentListError && documentList.map((documentItem) => (
                        <Link
                            className="mypage-document-item"
                            to={`/documents/${selectedDocumentType}/${documentItem.documentNum}`}
                            key={documentItem.documentNum}
                        >
                            <span className="mypage-document-icon" aria-hidden="true">
                                <FileText size={20} />
                            </span>
                            <span className="mypage-document-info">
                                <strong>{documentItem.documentTitle || '제목 없음'}</strong>
                                <span>작성일 {formatDocumentDate(documentItem.createdAt)}</span>
                            </span>
                            <ChevronRight size={19} aria-hidden="true" />
                        </Link>
                    ))}
                </div>

                <div className="mypage-document-write-action">
                    <Link
                        className="mypage-document-write-link"
                        to={`/documents/write?type=${selectedDocumentType}`}
                    >
                        문서 작성하기
                    </Link>
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
                                    <>
                                        <input type="text" value={nickname} onChange={(event) => setNickname(event.target.value)} aria-label="닉네임" autoFocus required />
                                        {nicknameMessage && <p className={`mypage-field-message ${isNicknameAvailable ? 'is-valid' : 'is-error'}`}>{isNicknameChecking ? '닉네임을 확인 중입니다.' : nicknameMessage}</p>}
                                    </>
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
                            <div className="mypage-password-input">
                                <input id="current-password" type={showCurrentPassword ? 'text' : 'password'} value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} placeholder="현재 비밀번호를 입력하세요" autoComplete="current-password" required />
                                <button type="button" onClick={() => setShowCurrentPassword((visible) => !visible)} aria-label={showCurrentPassword ? '현재 비밀번호 숨기기' : '현재 비밀번호 보기'}>{showCurrentPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button>
                            </div>
                        </div>
                        <div className="mypage-field">
                            <label htmlFor="new-password">새 비밀번호</label>
                            <div className="mypage-password-input">
                                <input id="new-password" type={showNewPassword ? 'text' : 'password'} value={newPassword} onChange={(event) => setNewPassword(event.target.value)} placeholder="새 비밀번호를 입력하세요" autoComplete="new-password" required />
                                <button type="button" onClick={() => setShowNewPassword((visible) => !visible)} aria-label={showNewPassword ? '새 비밀번호 숨기기' : '새 비밀번호 보기'}>{showNewPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button>
                            </div>
                            {isPasswordInvalid && <p className="mypage-error">8자 이상이며 영문 대소문자, 숫자, 특수문자를 각각 포함해야 합니다.</p>}
                            {isPasswordSameAsCurrent && <p className="mypage-error">새 비밀번호는 현재 비밀번호와 다르게 설정해 주세요.</p>}
                        </div>
                        <div className="mypage-field">
                            <label htmlFor="new-password-confirm">새 비밀번호 확인</label>
                            <div className="mypage-password-input">
                                <input id="new-password-confirm" type={showNewPasswordConfirm ? 'text' : 'password'} value={newPasswordConfirm} onChange={(event) => setNewPasswordConfirm(event.target.value)} placeholder="새 비밀번호를 다시 입력하세요" autoComplete="new-password" aria-describedby={isPasswordMismatch ? 'password-mismatch' : undefined} required />
                                <button type="button" onClick={() => setShowNewPasswordConfirm((visible) => !visible)} aria-label={showNewPasswordConfirm ? '새 비밀번호 확인 숨기기' : '새 비밀번호 확인 보기'}>{showNewPasswordConfirm ? <EyeOff size={18} /> : <Eye size={18} />}</button>
                            </div>
                            {isPasswordMismatch && <p id="password-mismatch" className="mypage-error">새 비밀번호가 일치하지 않습니다.</p>}
                        </div>
                    </section>
                )}

                <div className="mypage-actions">
                    <button type="button" className="mypage-cancel" onClick={handleCancel}>취소</button>
                    <button type="submit" className="mypage-save" disabled={isPasswordMismatch || isPasswordInvalid || isPasswordSameAsCurrent || isNicknameSaveBlocked || isSaving}>{isSaving ? '저장 중...' : '변경사항 저장하기'}</button>
                </div>

                {saveMessage && <p className="mypage-save-message" role="status">{saveMessage}</p>}

                <div className="mypage-withdrawal">
                    <button type="button" onClick={handleWithdrawalClick}>회원 탈퇴</button>
                </div>

                {isWithdrawalOpen && (
                    <section className="mypage-withdrawal-panel" aria-labelledby="withdrawal-title">
                        <h3 id="withdrawal-title">회원 탈퇴</h3>
                        <p>탈퇴하면 계정 이용이 즉시 중지되며 동일한 이메일로 재가입할 수 없습니다.</p>
                        <label htmlFor="withdrawal-password">현재 비밀번호</label>
                        <div className="mypage-password-input">
                            <input
                                id="withdrawal-password"
                                type={showWithdrawalPassword ? 'text' : 'password'}
                                value={withdrawalPassword}
                                onChange={(event) => setWithdrawalPassword(event.target.value)}
                                placeholder="현재 비밀번호를 입력하세요"
                                autoComplete="current-password"
                            />
                            <button type="button" onClick={() => setShowWithdrawalPassword((visible) => !visible)} aria-label={showWithdrawalPassword ? '현재 비밀번호 숨기기' : '현재 비밀번호 보기'}>{showWithdrawalPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button>
                        </div>
                        {withdrawalError && <p className="mypage-error">{withdrawalError}</p>}
                        <div className="mypage-withdrawal-actions">
                            <button type="button" className="mypage-cancel" onClick={closeWithdrawalPanel}>취소</button>
                            <button type="button" className="mypage-withdraw-confirm" onClick={handleWithdrawalSubmit} disabled={isWithdrawing}>{isWithdrawing ? '처리 중...' : '회원 탈퇴하기'}</button>
                        </div>
                    </section>
                )}
            </form>
        </div>
    );
}

function formatDocumentDate(value) {
    return value ? value.slice(0, 10).replaceAll('-', '.') : '-';
}

export default MyPage;
