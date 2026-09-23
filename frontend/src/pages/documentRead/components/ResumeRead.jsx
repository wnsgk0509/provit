import { Award, BriefcaseBusiness, GraduationCap, Pencil } from 'lucide-react';
import { Link } from 'react-router-dom';

function ResumeRead({ document }) {
    const { resume, educationList = [], careerList = [], certificationList = [] } = document;

    return (
        <article className="document-form document-read-card">
            <div className="document-form-heading document-read-heading">
                <div>
                    <span>RESUME</span>
                    <h2>{valueOrDash(resume.resumeTitle)}</h2>
                    <p>이력서에 저장된 기본 정보와 경력 사항입니다.</p>
                </div>
                <DocumentDate createdAt={resume.createdAt} updatedAt={resume.updatedAt} />
            </div>

            <section className="document-form-section" aria-labelledby="resume-read-basic-title">
                <div className="document-section-heading">
                    <h3 id="resume-read-basic-title">기본 정보</h3>
                </div>
                <dl className="document-read-grid">
                    <ReadValue label="최종 학력" value={resume.highestLevel} />
                    <ReadValue label="학력 구분" value={resume.educationName} />
                    <ReadValue label="희망 근무 지역" value={resume.desiredLocation} />
                    <ReadValue label="희망 근무 형태" value={resume.desiredWorkType} />
                    <ReadValue label="지원 동기" value={resume.motivation} wide multiline />
                </dl>
            </section>

            <ReadListSection title="학력" icon={<GraduationCap size={20} />} items={educationList}>
                {(education) => (
                    <dl className="document-read-grid">
                        <ReadValue label="학교명" value={education.schoolName} />
                        <ReadValue label="전공" value={education.major} />
                        <ReadValue label="입학일" value={formatDate(education.admissionDate)} />
                        <ReadValue label="졸업일" value={formatDate(education.graduationDate)} />
                        <ReadValue label="졸업 상태" value={education.educationStatus} wide />
                    </dl>
                )}
            </ReadListSection>

            <ReadListSection title="경력" icon={<BriefcaseBusiness size={20} />} items={careerList}>
                {(career) => (
                    <dl className="document-read-grid">
                        <ReadValue label="회사명" value={career.companyName} wide />
                        <ReadValue label="입사일" value={formatDate(career.joinDate)} />
                        <ReadValue label="퇴사일" value={formatDate(career.resignDate)} />
                        <ReadValue label="주요 업무" value={career.mainDuty} wide multiline />
                    </dl>
                )}
            </ReadListSection>

            <ReadListSection title="자격증" icon={<Award size={20} />} items={certificationList}>
                {(certification) => (
                    <dl className="document-read-grid">
                        <ReadValue label="자격증명" value={certification.certName} />
                        <ReadValue label="등급/점수" value={certification.certGrade} />
                        <ReadValue label="취득일" value={formatDate(certification.issueDate)} wide />
                    </dl>
                )}
            </ReadListSection>

            <div className="document-form-actions">
                <Link
                    className="document-primary-button document-read-edit-link"
                    to={`/documents/resume/${resume.resumeNum}/edit`}
                >
                    <Pencil size={17} /> 이력서 수정
                </Link>
            </div>
        </article>
    );
}

function ReadListSection({ title, icon, items, children }) {
    return (
        <section className="document-form-section">
            <div className="document-repeat-heading">
                <div className="document-repeat-title">
                    <span className="document-repeat-icon" aria-hidden="true">{icon}</span>
                    <div><h3>{title}</h3><p>등록된 {title} 정보를 확인하세요.</p></div>
                </div>
                <span className="document-read-count">총 {items.length}개</span>
            </div>
            {items.length === 0 ? (
                <div className="document-empty-state">등록된 {title} 정보가 없습니다.</div>
            ) : items.map((item, index) => (
                <div className="document-repeat-item" key={item.eduNum || item.careerNum || item.certNum}>
                    <div className="document-repeat-item-heading"><strong>{title} {index + 1}</strong></div>
                    {children(item)}
                </div>
            ))}
        </section>
    );
}

export function ReadValue({ label, value, wide = false, multiline = false }) {
    return (
        <div className={`document-read-value${wide ? ' is-wide' : ''}`}>
            <dt>{label}</dt>
            <dd className={multiline ? 'is-multiline' : ''}>{valueOrDash(value)}</dd>
        </div>
    );
}

export function DocumentDate({ createdAt, updatedAt }) {
    return (
        <dl className="document-read-dates">
            <div><dt>작성일</dt><dd>{formatDateTime(createdAt)}</dd></div>
            {updatedAt && updatedAt !== createdAt && (
                <div><dt>수정일</dt><dd>{formatDateTime(updatedAt)}</dd></div>
            )}
        </dl>
    );
}

function valueOrDash(value) {
    return value === null || value === undefined || value === '' ? '-' : value;
}

function formatDate(value) {
    return value ? value.slice(0, 10).replaceAll('-', '.') : '-';
}

function formatDateTime(value) {
    return value ? value.slice(0, 16).replace('T', ' ').replaceAll('-', '.') : '-';
}

export default ResumeRead;
