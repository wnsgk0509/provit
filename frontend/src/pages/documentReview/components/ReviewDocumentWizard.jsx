import { useEffect, useRef, useState } from 'react';
import { ArrowLeft, ArrowRight, Check, CheckCircle2 } from 'lucide-react';
import { DOCUMENT_REVIEW_TYPES } from '../documentReviewConfig';
import ReviewDocumentSelector from './ReviewDocumentSelector';

function ReviewDocumentWizard({ selections, isComplete, onChange, onComplete, onEdit, onContinue, disabled }) {
    const [step, setStep] = useState(0);
    const panelHeadingRef = useRef(null);
    const resumeReady = Boolean(selections.resumeNum.source.document);
    const letterReady = Boolean(selections.letterNum.source.document);
    const firstIncompleteStep = !resumeReady ? 0 : !letterReady ? 1 : 2;
    const activeStep = step === 3 && isComplete ? 3 : Math.min(step, firstIncompleteStep);
    const previousStepRef = useRef(activeStep);
    const config = DOCUMENT_REVIEW_TYPES[activeStep];
    const selection = config ? selections[config.selectionKey] : null;
    const canAdvance = Boolean(selection?.source.document);

    useEffect(() => {
        if (previousStepRef.current !== activeStep) panelHeadingRef.current?.focus();
        previousStepRef.current = activeStep;
    }, [activeStep]);

    const moveTo = (nextStep) => {
        onEdit();
        setStep(nextStep);
    };
    const complete = (skipPortfolio) => {
        if (!resumeReady || !letterReady || (!skipPortfolio && !canAdvance)) return;
        onComplete(skipPortfolio);
        setStep(3);
    };
    const steps = [...DOCUMENT_REVIEW_TYPES.map((type) => type.label), '서류 선택 완료'];

    return (
        <section className="review-stage review-document-wizard" aria-labelledby="review-document-title">
            <div className="review-card-heading">
                <div>
                    <h2 id="review-document-title" tabIndex={-1}>첨삭할 서류 선택</h2>
                    <p>이력서부터 순서대로 선택해 주세요. 포트폴리오는 생략할 수 있습니다.</p>
                </div>
            </div>
            <ol className="review-selection-steps" aria-label="서류 선택 단계">
                {steps.map((label, index) => (
                    <li
                        key={label}
                        className={index === activeStep ? 'is-current' : index < activeStep ? 'is-done' : ''}
                    >
                        <button
                            type="button"
                            aria-label={`${index + 1}. ${label}`}
                            aria-current={index === activeStep ? 'step' : undefined}
                            disabled={disabled || index >= activeStep}
                            onClick={() => moveTo(index)}
                        >
                            <span className="review-selection-step-number">
                                {index < activeStep ? <Check size={13} aria-hidden="true" /> : index + 1}
                            </span>
                            <span>
                                {label}
                                {index === 2 && <small>생략 가능</small>}
                            </span>
                        </button>
                    </li>
                ))}
            </ol>
            <div className="review-selection-panel">
                <h3 className="review-selection-panel-title" ref={panelHeadingRef} tabIndex={-1}>
                    {activeStep === 3 ? '서류 선택이 완료되었습니다.' : `${activeStep + 1}. ${config.label} 선택`}
                </h3>
                {activeStep === 3 ? (
                    <>
                        <p className="review-selection-complete-message" role="status">
                            <CheckCircle2 size={17} aria-hidden="true" /> 선택한 서류를 함께 첨삭합니다.
                        </p>
                        <ul className="review-selection-summary">
                            {DOCUMENT_REVIEW_TYPES.map((type, index) => {
                                const item = selections[type.selectionKey];
                                const document = item.documents.find(
                                    (document) => String(document.documentNum) === item.selectedDocumentNum,
                                );
                                return (
                                    <li key={type.value}>
                                        <div>
                                            <span>{type.label}</span>
                                            <strong>
                                                {document?.documentTitle || (type.required ? '제목 없음' : '생략')}
                                            </strong>
                                        </div>
                                        <button
                                            type="button"
                                            className="review-text-button"
                                            aria-label={`${type.label} 선택 변경`}
                                            onClick={() => moveTo(index)}
                                            disabled={disabled}
                                        >
                                            변경
                                        </button>
                                    </li>
                                );
                            })}
                        </ul>
                        <p className="review-field-hint">선택한 서류를 확인한 뒤 다음 단계에서 첨삭 기준을 설정해 주세요.</p>
                        <div className="review-selection-actions">
                            <button
                                type="button"
                                className="review-primary-button"
                                onClick={onContinue}
                                disabled={disabled || !isComplete}
                            >
                                다음: 첨삭 기준 설정 <ArrowRight size={15} aria-hidden="true" />
                            </button>
                        </div>
                    </>
                ) : (
                    <>
                        <ReviewDocumentSelector
                            key={config.value}
                            config={config}
                            selection={selection}
                            onChange={(value) => onChange(config.selectionKey, value)}
                            disabled={disabled}
                        />
                        <div className="review-selection-actions">
                            {activeStep > 0 && (
                                <button
                                    type="button"
                                    className="review-secondary-button"
                                    onClick={() => moveTo(activeStep - 1)}
                                    disabled={disabled}
                                >
                                    <ArrowLeft size={15} aria-hidden="true" /> 이전
                                </button>
                            )}
                            {activeStep < 2 ? (
                                <button
                                    type="button"
                                    className="review-primary-button"
                                    onClick={() => setStep(activeStep + 1)}
                                    disabled={disabled || !canAdvance}
                                >
                                    다음: {DOCUMENT_REVIEW_TYPES[activeStep + 1].label}
                                    <ArrowRight size={15} aria-hidden="true" />
                                </button>
                            ) : (
                                <>
                                    <button
                                        type="button"
                                        className="review-secondary-button review-skip-portfolio"
                                        onClick={() => complete(true)}
                                        disabled={disabled || !resumeReady || !letterReady}
                                    >
                                        포트폴리오 생략하고 완료
                                    </button>
                                    <button
                                        type="button"
                                        className="review-primary-button"
                                        onClick={() => complete(false)}
                                        disabled={disabled || !canAdvance || !resumeReady || !letterReady}
                                    >
                                        서류 선택 완료 <Check size={15} aria-hidden="true" />
                                    </button>
                                </>
                            )}
                        </div>
                    </>
                )}
            </div>
        </section>
    );
}

export default ReviewDocumentWizard;
