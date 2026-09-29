import { ArrowLeft, Layers, Sparkles } from 'lucide-react';
import { REVIEW_CUSTOM_MAX_LENGTH, REVIEW_INSTRUCTIONS_MAX_LENGTH, REVIEW_MODE_OPTIONS } from '../documentReviewConfig';

function ReviewOptions({
    reviewMode,
    customCriteria,
    instructions,
    canReview,
    isSubmitting,
    onModeChange,
    onCustomCriteriaChange,
    onInstructionsChange,
    onPrevious,
    onPreview,
}) {
    const selectedMode = REVIEW_MODE_OPTIONS.find((option) => option.value === reviewMode);
    const isCustom = reviewMode === 'custom';
    const customError = isCustom && customCriteria.length > 0 && !customCriteria.trim();

    return (
        <section className="review-stage" data-review-step="1" aria-labelledby="review-options-title">
            <div className="review-card-heading">
                <div>
                    <h2 id="review-options-title" tabIndex={-1}>통합 첨삭 기준 설정</h2>
                    <p>선택한 모든 서류에 같은 기준과 추가 요청을 적용합니다.</p>
                </div>
            </div>
            <div className="review-consistency-guide">
                <Layers size={16} aria-hidden="true" />
                <span>선택한 기준과 추가 요청을 서류 묶음에 함께 기록합니다.</span>
            </div>
            <div className="review-mode-field">
                <label htmlFor="review-mode">첨삭 기준</label>
                <select
                    id="review-mode"
                    value={reviewMode}
                    onChange={(event) => onModeChange(event.target.value)}
                    aria-describedby="review-mode-description"
                    disabled={isSubmitting}
                    required
                >
                    {REVIEW_MODE_OPTIONS.map((option) => (
                        <option key={option.value} value={option.value}>
                            {option.label}
                        </option>
                    ))}
                </select>
                <p id="review-mode-description" className="review-mode-description">
                    {selectedMode?.description}
                </p>
            </div>
            {isCustom && (
                <div className="review-custom-criteria">
                    <label htmlFor="review-custom-criteria">
                        직접 입력한 첨삭 기준 <span>필수</span>
                    </label>
                    <textarea
                        id="review-custom-criteria"
                        value={customCriteria}
                        onChange={(event) => onCustomCriteriaChange(event.target.value)}
                        maxLength={REVIEW_CUSTOM_MAX_LENGTH}
                        rows={4}
                        placeholder="예: 프로젝트에서 맡은 역할과 문제 해결 과정을 중심으로 첨삭해 주세요."
                        aria-describedby={`review-custom-count${customError ? ' review-custom-error' : ''}`}
                        aria-invalid={customError || undefined}
                        disabled={isSubmitting}
                        required
                    />
                    <span id="review-custom-count" className="review-character-count">
                        {customCriteria.length} / {REVIEW_CUSTOM_MAX_LENGTH}자
                    </span>
                    {customError && (
                        <p id="review-custom-error" className="review-field-error" role="alert">
                            공백을 제외한 첨삭 기준을 입력해 주세요.
                        </p>
                    )}
                </div>
            )}
            <div className="review-instructions">
                <label htmlFor="review-instructions">
                    추가 요청 <span>선택</span>
                </label>
                <textarea
                    id="review-instructions"
                    value={instructions}
                    onChange={(event) => onInstructionsChange(event.target.value)}
                    maxLength={REVIEW_INSTRUCTIONS_MAX_LENGTH}
                    rows={4}
                    placeholder="예: 백엔드 개발자 지원용으로, 각 서류에 적은 프로젝트 역할과 성과가 일관되는지 봐 주세요."
                    disabled={isSubmitting}
                />
                <span className="review-character-count">
                    {instructions.length} / {REVIEW_INSTRUCTIONS_MAX_LENGTH}자
                </span>
            </div>
            <div className="review-stage-actions">
                <button type="button" className="review-secondary-button" onClick={onPrevious} disabled={isSubmitting}>
                    <ArrowLeft size={16} aria-hidden="true" /> 서류 선택으로
                </button>
                <button type="button" className="review-secondary-button" onClick={onPreview} disabled={isSubmitting}>
                    통합 결과 예시 보기
                </button>
                <button
                    type="submit"
                    className="review-primary-button"
                    disabled={!canReview || isSubmitting}
                    aria-describedby="review-submit-hint"
                >
                    <Sparkles size={18} aria-hidden="true" />
                    {isSubmitting ? '더미 결과 저장 중...' : '더미 첨삭 결과 저장하기'}
                </button>
            </div>
            <p id="review-submit-hint" className="review-submit-hint">
                AI 호출 없이 예시 결과를 저장합니다. 기준과 요청은 기록되며, 더미 내용에는 반영되지 않습니다.
            </p>
        </section>
    );
}

export default ReviewOptions;
