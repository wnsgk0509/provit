const INTERVIEW_STYLES = [
    { value: 'RANDOM', label: '랜덤면접' },
    { value: 'ONE_TO_ONE', label: '일대일면접' },
    { value: 'PANEL', label: '다대일면접' },
    { value: 'GROUP', label: '다대다면접' },
];

const DIFFICULTIES = [
    { value: 'HARD', label: '압박면접' },
    { value: 'NORMAL', label: '심층면접' },
    { value: 'EASY', label: '일반면접' },
];

const formatDate = (dateTime) => dateTime?.split(' ')[0] ?? '';

const getPortfolioName = (fileUrl) => {
    if (!fileUrl) {
        return '등록된 포트폴리오';
    }

    const normalizedUrl = fileUrl.replaceAll('\\', '/');
    const fileName = normalizedUrl.split('/').pop();

    try {
        return decodeURIComponent(fileName) || '등록된 포트폴리오';
    } catch {
        return fileName || '등록된 포트폴리오';
    }
};

function InterviewCustom({
    settings,
    documents,
    isDocumentLoading,
    isLoading,
    onSettingChange,
    onStart,
}) {
    const hasResume = documents.resumeList.length > 0;
    const hasPortfolio = Boolean(documents.portfolio);
    const hasCoverLetter = Boolean(documents.coverLetter);
    const jobPreference = documents.jobPreference;

    const handleSubmit = (event) => {
        event.preventDefault();
        onStart();
    };

    return (
        <section className="interview-custom">
            <div className="interview-section-heading">
                <span>STEP 1</span>
                <h2>면접 커스텀</h2>
                <p>면접에 사용할 서류와 진행 방식을 선택해 주세요.</p>
            </div>

            {jobPreference && (
                <div className="interview-job-summary">
                    <span>희망 직군</span>
                    <strong>{jobPreference.occupationName || '미설정'}</strong>
                    <span>희망 직무</span>
                    <strong>{jobPreference.jobName || '미설정'}</strong>
                </div>
            )}

            <form onSubmit={handleSubmit}>
                <div className="interview-form-group">
                    <label htmlFor="interview-resume">이력서</label>
                    <select
                        id="interview-resume"
                        value={settings.resumeNum}
                        onChange={(event) => onSettingChange('resumeNum', event.target.value)}
                        disabled={isDocumentLoading || !hasResume}
                        required
                    >
                        <option value="">
                            {isDocumentLoading ? '이력서를 불러오는 중입니다' : '이력서를 선택해 주세요'}
                        </option>
                        {documents.resumeList.map((resume) => (
                            <option key={resume.resumeNum} value={resume.resumeNum}>
                                {`이력서 #${resume.resumeNum} · ${resume.educationName} · ${formatDate(resume.updatedAt)}`}
                            </option>
                        ))}
                    </select>
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-portfolio">포트폴리오</label>
                    <select
                        id="interview-portfolio"
                        value={String(settings.usePortfolio)}
                        onChange={(event) => onSettingChange('usePortfolio', event.target.value === 'true')}
                        disabled={isDocumentLoading || !hasPortfolio}
                    >
                        <option value="false">사용하지 않음</option>
                        {hasPortfolio && (
                            <option value="true">{getPortfolioName(documents.portfolio.fileUrl)}</option>
                        )}
                    </select>
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-cover-letter">자기소개서</label>
                    <select
                        id="interview-cover-letter"
                        value={String(settings.useCoverLetter)}
                        onChange={(event) => onSettingChange('useCoverLetter', event.target.value === 'true')}
                        disabled={isDocumentLoading || !hasCoverLetter}
                    >
                        <option value="false">사용하지 않음</option>
                        {hasCoverLetter && <option value="true">등록된 자기소개서</option>}
                    </select>
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-style">면접 스타일</label>
                    <select
                        id="interview-style"
                        value={settings.interviewStyle}
                        onChange={(event) => onSettingChange('interviewStyle', event.target.value)}
                        required
                    >
                        <option value="">면접 스타일을 선택해 주세요</option>
                        {INTERVIEW_STYLES.map((style) => (
                            <option key={style.value} value={style.value}>{style.label}</option>
                        ))}
                    </select>
                </div>

                <fieldset className="interview-form-group interview-difficulty">
                    <legend>면접 난이도</legend>
                    <div className="interview-radio-group">
                        {DIFFICULTIES.map((difficulty) => (
                            <label key={difficulty.value} htmlFor={`difficulty-${difficulty.value}`}>
                                <input
                                    id={`difficulty-${difficulty.value}`}
                                    type="radio"
                                    name="interview-difficulty"
                                    value={difficulty.value}
                                    checked={settings.interviewDifficulty === difficulty.value}
                                    onChange={(event) => onSettingChange('interviewDifficulty', event.target.value)}
                                    required
                                />
                                <span>{difficulty.label}</span>
                            </label>
                        ))}
                    </div>
                </fieldset>

                {!isDocumentLoading && !hasResume && (
                    <p className="interview-document-empty">등록된 이력서가 없어 면접을 시작할 수 없습니다.</p>
                )}

                <button
                    className="btn btn-primary interview-primary-button"
                    type="submit"
                    disabled={isLoading || isDocumentLoading || !hasResume}
                >
                    {isLoading ? '면접 준비 중...' : '면접 시작'}
                </button>
            </form>
        </section>
    );
}

export default InterviewCustom;
