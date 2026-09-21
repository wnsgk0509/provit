const PORTFOLIOS = [
    { documentId: 'portfolio-1', title: '웹 서비스 프로젝트 포트폴리오' },
    { documentId: 'portfolio-2', title: '백엔드 개발 포트폴리오' },
];

const COVER_LETTERS = [
    { documentId: 'cover-letter-1', title: '신입 개발자 자기소개서' },
    { documentId: 'cover-letter-2', title: '프로젝트 중심 자기소개서' },
];

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

function InterviewCustom({ settings, isLoading, onSettingChange, onStart }) {
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

            <form onSubmit={handleSubmit}>
                <div className="interview-form-group">
                    <label htmlFor="interview-portfolio">포트폴리오</label>
                    <select
                        id="interview-portfolio"
                        value={settings.portfolioDocumentId}
                        onChange={(event) => onSettingChange('portfolioDocumentId', event.target.value)}
                        required
                    >
                        <option value="">포트폴리오를 선택해 주세요</option>
                        {PORTFOLIOS.map((portfolio) => (
                            <option key={portfolio.documentId} value={portfolio.documentId}>{portfolio.title}</option>
                        ))}
                    </select>
                </div>

                <div className="interview-form-group">
                    <label htmlFor="interview-cover-letter">자기소개서</label>
                    <select
                        id="interview-cover-letter"
                        value={settings.coverLetterDocumentId}
                        onChange={(event) => onSettingChange('coverLetterDocumentId', event.target.value)}
                        required
                    >
                        <option value="">자기소개서를 선택해 주세요</option>
                        {COVER_LETTERS.map((coverLetter) => (
                            <option key={coverLetter.documentId} value={coverLetter.documentId}>{coverLetter.title}</option>
                        ))}
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
                                    checked={settings.difficulty === difficulty.value}
                                    onChange={(event) => onSettingChange('difficulty', event.target.value)}
                                    required
                                />
                                <span>{difficulty.label}</span>
                            </label>
                        ))}
                    </div>
                </fieldset>

                <button className="btn btn-primary interview-primary-button" type="submit" disabled={isLoading}>
                    {isLoading ? '면접 준비 중...' : '면접 시작'}
                </button>
            </form>
        </section>
    );
}

export default InterviewCustom;
