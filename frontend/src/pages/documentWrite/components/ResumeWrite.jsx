import { useEffect, useState } from 'react';
import { Award, BriefcaseBusiness, GraduationCap, Plus, Trash2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { createResume, updateResume } from '../../../api/documentApi';
import { fetchOccupations, fetchJobsByOccupation } from '../../../api/recruitmentApi';
import { getResumeDateError, getTodayInSeoul } from '../resumeDateValidation';
import { useModal } from '../../../context/ModalContext';

const emptyResume = {
    resumeTitle: '', highestLevel: '', motivation: '',
    desiredLocation: '', desiredWorkType: '',
    occupationCode: '', jobCode: '',
};
const emptyEducation = {
    schoolName: '', admissionDate: '', graduationDate: '', major: '', educationStatus: '',
};
const emptyCareer = { companyName: '', joinDate: '', resignDate: '', mainDuty: '' };
const emptyCertification = { certName: '', certGrade: '', issueDate: '' };

function ResumeWrite({ initialData = null, onSaved, onCancel }) {
    const navigate = useNavigate();
    const { showToast } = useModal();
    const isEditMode = Boolean(initialData?.resume?.resumeNum);
    const [resume, setResume] = useState(() => normalizeFormItem(emptyResume, initialData?.resume));
    const [educations, setEducations] = useState(() => (
        initialData
            ? normalizeFormList(emptyEducation, initialData.educationList)
            : [{ ...emptyEducation }]
    ));
    const [careers, setCareers] = useState(() => (
        normalizeFormList(emptyCareer, initialData?.careerList)
    ));
    const [certifications, setCertifications] = useState(() => (
        normalizeFormList(emptyCertification, initialData?.certificationList)
    ));
    const [isSaving, setIsSaving] = useState(false);
    const [saveMessage, setSaveMessage] = useState({ type: '', text: '' });
    const [occupations, setOccupations] = useState([]);
    const [jobOptions, setJobOptions] = useState({ occupationCode: '', items: [] });
    const [occupationError, setOccupationError] = useState('');
    const [jobError, setJobError] = useState('');
    const [retryCount, setRetryCount] = useState(0);
    const jobsReady = Boolean(resume.occupationCode)
        && jobOptions.occupationCode === resume.occupationCode;
    const selectedJobValid = jobsReady && jobOptions.items.some((job) => job.jobCode === resume.jobCode);
    const today = getTodayInSeoul();

    useEffect(() => {
        let active = true;
        fetchOccupations().then((response) => {
            if (!Array.isArray(response?.data)) throw new Error('Invalid occupations');
            if (active) { setOccupations(response.data); setOccupationError(''); }
        }).catch(() => {
            if (active) setOccupationError('직군 목록을 불러오지 못했습니다. 다시 시도해 주세요.');
        });
        return () => { active = false; };
    }, [retryCount]);

    useEffect(() => {
        if (!resume.occupationCode) return;
        let active = true;
        const occupationCode = resume.occupationCode;
        fetchJobsByOccupation(occupationCode).then((response) => {
            if (!Array.isArray(response?.data)) throw new Error('Invalid jobs');
            if (active) {
                setJobOptions({ occupationCode, items: response.data });
                setJobError('');
            }
        }).catch(() => {
            if (active) setJobError('직무 목록을 불러오지 못했습니다. 다시 시도해 주세요.');
        });
        return () => { active = false; };
    }, [resume.occupationCode, retryCount]);

    const handleResumeChange = (event) => {
        const { name, value } = event.target;
        if (name === 'occupationCode') {
            setResume((current) => ({ ...current, occupationCode: value, jobCode: '' }));
            setJobOptions({ occupationCode: '', items: [] });
            setJobError('');
            return;
        }
        setResume((current) => ({ ...current, [name]: value }));
    };

    const changeListItem = (setter, index, name, value) => {
        const dateError = getResumeDateError(name, value);
        if (dateError) {
            setSaveMessage({ type: 'error', text: dateError });
            return;
        }
        setter((items) => items.map((item, itemIndex) => (
            itemIndex === index ? { ...item, [name]: value } : item
        )));
    };

    const removeListItem = (setter, index) => {
        setter((items) => items.filter((_, itemIndex) => itemIndex !== index));
    };

    const emptyDateToNull = (item, dateFields) => {
        const normalizedItem = { ...item };
        dateFields.forEach((field) => {
            normalizedItem[field] = normalizedItem[field] || null;
        });
        return normalizedItem;
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        const submissionDate = getTodayInSeoul();
        const dateError = [...educations, ...careers, ...certifications]
            .flatMap((item) => Object.entries(item)
                .map(([field, value]) => getResumeDateError(field, value, submissionDate)))
            .find(Boolean);
        if (dateError) {
            setSaveMessage({ type: 'error', text: dateError });
            return;
        }
        if (!selectedJobValid || occupationError || jobError) {
            setSaveMessage({ type: 'error', text: '직군과 해당 직군에 속한 직무를 선택해 주세요.' });
            return;
        }
        setIsSaving(true);
        setSaveMessage({ type: '', text: '' });

        const payload = {
            resume: { ...resume },
            educationList: educations.map((education) => (
                emptyDateToNull(education, ['admissionDate', 'graduationDate'])
            )),
            careerList: careers.map((career) => (
                emptyDateToNull(career, ['joinDate', 'resignDate'])
            )),
            certificationList: certifications.map((certification) => (
                emptyDateToNull(certification, ['issueDate'])
            )),
        };

        try {
            const savedResume = isEditMode
                ? await updateResume(initialData.resume.resumeNum, payload)
                : await createResume(payload);
            const resumeNum = savedResume?.resume?.resumeNum;
            if (isEditMode) {
                onSaved?.(savedResume);
                return;
            }
            if (!resumeNum) throw new Error('저장된 이력서 번호를 확인하지 못했습니다.');
            showToast('이력서가 저장되었습니다.', 'success');
            navigate(`/documents/resume/${resumeNum}`, { replace: true });
            window.requestAnimationFrame(() => window.scrollTo({ top: 0, behavior: 'auto' }));
        } catch (error) {
            const responseData = error.response?.data;
            setSaveMessage({
                type: 'error',
                text: responseData?.data
                    || responseData?.responseCode?.message
                    || `이력서를 ${isEditMode ? '수정' : '저장'}하지 못했습니다. 잠시 후 다시 시도해 주세요.`,
            });
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <form className="document-form" onSubmit={handleSubmit}>
            <div className="document-form-heading">
                <span>RESUME</span>
                <h2>이력서 {isEditMode ? '수정' : '작성'}</h2>
                <p>기본 정보와 학력, 경력, 자격증을 {isEditMode ? '수정' : '입력'}해 이력서를 구성하세요.</p>
            </div>

            <section className="document-form-section" aria-labelledby="resume-basic-title">
                <div className="document-section-heading">
                    <h3 id="resume-basic-title">기본 정보</h3>
                    <span>필수 항목을 먼저 입력해 주세요.</span>
                </div>
                <div className="document-field-grid">
                    <div className="document-field document-field-wide">
                        <label htmlFor="resumeTitle">이력서 제목 <b>*</b></label>
                        <input id="resumeTitle" name="resumeTitle" value={resume.resumeTitle} onChange={handleResumeChange} maxLength="200" placeholder="예: 백엔드 개발자 지원 이력서" required />
                    </div>
                    <div className="document-field">
                        <label htmlFor="occupationCode">지원 직군 (1차 직종) <b>*</b></label>
                        <select id="occupationCode" name="occupationCode" value={resume.occupationCode} onChange={handleResumeChange} disabled={isSaving || occupations.length === 0} required>
                            <option value="">직군을 선택해 주세요</option>
                            {occupations.map((occupation) => (
                                <option key={occupation.occupationCode} value={occupation.occupationCode}>{occupation.occupationName}</option>
                            ))}
                        </select>
                    </div>
                    <div className="document-field">
                        <label htmlFor="jobCode">지원 직무 (2차 직종) <b>*</b></label>
                        <select id="jobCode" name="jobCode" value={selectedJobValid ? resume.jobCode : ''} onChange={handleResumeChange} disabled={isSaving || !jobsReady || Boolean(jobError)} required>
                            <option value="">{!resume.occupationCode ? '직군을 먼저 선택해 주세요' : !jobsReady ? '직무 목록 불러오는 중...' : '직무를 선택해 주세요'}</option>
                            {(jobsReady ? jobOptions.items : []).map((job) => (
                                <option key={job.jobCode} value={job.jobCode}>{job.jobName}</option>
                            ))}
                        </select>
                    </div>
                    {(occupationError || jobError) && (
                        <div className="document-field document-field-wide" role="alert">
                            <p>{occupationError || jobError}</p>
                            <button type="button" onClick={() => setRetryCount((count) => count + 1)}>목록 다시 불러오기</button>
                        </div>
                    )}
                    <div className="document-field">
                        <label htmlFor="highestLevel">최종 학력 <b>*</b></label>
                        <select id="highestLevel" name="highestLevel" value={resume.highestLevel} onChange={handleResumeChange} required>
                            <option value="">선택해 주세요</option>
                            <option value="고등학교">고등학교</option>
                            <option value="전문대학">전문대학</option>
                            <option value="대학교">대학교</option>
                            <option value="대학원">대학원</option>
                        </select>
                    </div>
                    <div className="document-field">
                        <label htmlFor="desiredLocation">희망 근무 지역</label>
                        <input id="desiredLocation" name="desiredLocation" value={resume.desiredLocation} onChange={handleResumeChange} maxLength="200" placeholder="예: 서울특별시" />
                    </div>
                    <div className="document-field">
                        <label htmlFor="desiredWorkType">희망 근무 형태</label>
                        <select id="desiredWorkType" name="desiredWorkType" value={resume.desiredWorkType} onChange={handleResumeChange}>
                            <option value="">선택해 주세요</option>
                            <option value="정규직">정규직</option>
                            <option value="계약직">계약직</option>
                            <option value="인턴">인턴</option>
                            <option value="프리랜서">프리랜서</option>
                        </select>
                    </div>
                    <div className="document-field document-field-wide">
                        <label htmlFor="motivation">지원 동기</label>
                        <textarea className="document-fixed-textarea" id="motivation" name="motivation" value={resume.motivation} onChange={handleResumeChange} rows="6" placeholder="직무와 회사에 지원한 동기를 작성해 주세요." />
                    </div>
                </div>
            </section>

            <RepeatSection
                title="학력"
                description="학교별 재학 정보를 입력해 주세요."
                icon={<GraduationCap size={20} />}
                items={educations}
                addLabel="학력 추가"
                onAdd={() => setEducations((items) => [...items, { ...emptyEducation }])}
                onRemove={(index) => removeListItem(setEducations, index)}
            >
                {(education, index) => (
                    <div className="document-field-grid">
                        <div className="document-field document-field-wide">
                            <label htmlFor={`schoolName-${index}`}>학교명 <b>*</b></label>
                            <input id={`schoolName-${index}`} value={education.schoolName} onChange={(event) => changeListItem(setEducations, index, 'schoolName', event.target.value)} maxLength="200" placeholder="학교명을 입력해 주세요." required />
                        </div>
                        <div className="document-field">
                            <label htmlFor={`admissionDate-${index}`}>입학일</label>
                            <input id={`admissionDate-${index}`} type="date" max={today} value={education.admissionDate} onChange={(event) => changeListItem(setEducations, index, 'admissionDate', event.target.value)} />
                        </div>
                        <div className="document-field">
                            <label htmlFor={`graduationDate-${index}`}>졸업(예정)일</label>
                            <input id={`graduationDate-${index}`} type="date" value={education.graduationDate} onChange={(event) => changeListItem(setEducations, index, 'graduationDate', event.target.value)} />
                        </div>
                        <div className="document-field">
                            <label htmlFor={`major-${index}`}>전공</label>
                            <input id={`major-${index}`} value={education.major} onChange={(event) => changeListItem(setEducations, index, 'major', event.target.value)} maxLength="200" placeholder="예: 컴퓨터공학과" />
                        </div>
                        <div className="document-field">
                            <label htmlFor={`educationStatus-${index}`}>졸업 상태 <b>*</b></label>
                            <select id={`educationStatus-${index}`} value={education.educationStatus} onChange={(event) => changeListItem(setEducations, index, 'educationStatus', event.target.value)} required>
                                <option value="">선택해 주세요</option>
                                <option value="졸업">졸업</option>
                                <option value="졸업예정">졸업 예정</option>
                                <option value="재학">재학</option>
                                <option value="휴학">휴학</option>
                                <option value="중퇴">중퇴</option>
                            </select>
                        </div>
                    </div>
                )}
            </RepeatSection>

            <RepeatSection
                title="경력"
                description="경력이 없다면 비워 두어도 됩니다."
                icon={<BriefcaseBusiness size={20} />}
                items={careers}
                addLabel="경력 추가"
                onAdd={() => setCareers((items) => [...items, { ...emptyCareer }])}
                onRemove={(index) => removeListItem(setCareers, index)}
            >
                {(career, index) => (
                    <div className="document-field-grid">
                        <div className="document-field document-field-wide">
                            <label htmlFor={`companyName-${index}`}>회사명 <b>*</b></label>
                            <input id={`companyName-${index}`} value={career.companyName} onChange={(event) => changeListItem(setCareers, index, 'companyName', event.target.value)} maxLength="200" placeholder="회사명을 입력해 주세요." required />
                        </div>
                        <div className="document-field">
                            <label htmlFor={`joinDate-${index}`}>입사일</label>
                            <input id={`joinDate-${index}`} type="date" max={today} value={career.joinDate} onChange={(event) => changeListItem(setCareers, index, 'joinDate', event.target.value)} />
                        </div>
                        <div className="document-field">
                            <label htmlFor={`resignDate-${index}`}>퇴사(예정)일</label>
                            <input id={`resignDate-${index}`} type="date" value={career.resignDate} onChange={(event) => changeListItem(setCareers, index, 'resignDate', event.target.value)} />
                        </div>
                        <div className="document-field document-field-wide">
                            <label htmlFor={`mainDuty-${index}`}>주요 업무</label>
                            <textarea id={`mainDuty-${index}`} value={career.mainDuty} onChange={(event) => changeListItem(setCareers, index, 'mainDuty', event.target.value)} maxLength="2000" rows="4" placeholder="담당 업무와 성과를 작성해 주세요." />
                        </div>
                    </div>
                )}
            </RepeatSection>

            <RepeatSection
                title="자격증"
                description="보유한 자격증과 취득 정보를 입력해 주세요."
                icon={<Award size={20} />}
                items={certifications}
                addLabel="자격증 추가"
                onAdd={() => setCertifications((items) => [...items, { ...emptyCertification }])}
                onRemove={(index) => removeListItem(setCertifications, index)}
            >
                {(certification, index) => (
                    <div className="document-field-grid">
                        <div className="document-field">
                            <label htmlFor={`certName-${index}`}>자격증명 <b>*</b></label>
                            <input id={`certName-${index}`} value={certification.certName} onChange={(event) => changeListItem(setCertifications, index, 'certName', event.target.value)} maxLength="200" placeholder="예: 정보처리기사" required />
                        </div>
                        <div className="document-field">
                            <label htmlFor={`certGrade-${index}`}>등급/점수</label>
                            <input id={`certGrade-${index}`} value={certification.certGrade} onChange={(event) => changeListItem(setCertifications, index, 'certGrade', event.target.value)} maxLength="100" placeholder="예: 1급" />
                        </div>
                        <div className="document-field">
                            <label htmlFor={`issueDate-${index}`}>취득일</label>
                            <input id={`issueDate-${index}`} type="date" max={today} value={certification.issueDate} onChange={(event) => changeListItem(setCertifications, index, 'issueDate', event.target.value)} />
                        </div>
                    </div>
                )}
            </RepeatSection>

            <div className="document-form-actions">
                {onCancel && (
                    <button type="button" className="document-secondary-button" onClick={onCancel} disabled={isSaving}>
                        취소
                    </button>
                )}
                <button type="submit" className="document-primary-button" disabled={isSaving || !selectedJobValid || Boolean(occupationError || jobError)}>
                    {isSaving ? '저장 중...' : `이력서 ${isEditMode ? '수정' : '저장'}`}
                </button>
            </div>
            {saveMessage.text && (
                <p className={`document-save-message is-${saveMessage.type}`} role="status">
                    {saveMessage.text}
                </p>
            )}
        </form>
    );
}

function normalizeFormItem(template, item) {
    return Object.keys(template).reduce((normalized, key) => ({
        ...normalized,
        [key]: item?.[key] ?? '',
    }), {});
}

function normalizeFormList(template, items) {
    return (items || []).map((item) => normalizeFormItem(template, item));
}

function RepeatSection({ title, description, icon, items, addLabel, onAdd, onRemove, children }) {
    return (
        <section className="document-form-section">
            <div className="document-repeat-heading">
                <div className="document-repeat-title">
                    <span className="document-repeat-icon" aria-hidden="true">{icon}</span>
                    <div><h3>{title}</h3><p>{description}</p></div>
                </div>
                <button type="button" className="document-add-button" onClick={onAdd}>
                    <Plus size={17} /> {addLabel}
                </button>
            </div>

            {items.length === 0 ? (
                <div className="document-empty-state">등록된 {title} 정보가 없습니다.</div>
            ) : items.map((item, index) => (
                <div className="document-repeat-item" key={`${title}-${index}`}>
                    <div className="document-repeat-item-heading">
                        <strong>{title} {index + 1}</strong>
                        {onRemove && (
                            <button type="button" className="document-remove-button" onClick={() => onRemove(index)} aria-label={`${title} ${index + 1} 삭제`}>
                                <Trash2 size={16} /> 삭제
                            </button>
                        )}
                    </div>
                    {children(item, index)}
                </div>
            ))}
        </section>
    );
}

export default ResumeWrite;
