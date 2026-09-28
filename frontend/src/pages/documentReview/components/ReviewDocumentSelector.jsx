import { ArrowRight, CheckCircle2, FileText, LoaderCircle, RotateCcw } from 'lucide-react';
import { Link } from 'react-router-dom';
import DocumentSourcePreview from './DocumentSourcePreview';

function ReviewDocumentSelector({ config, selection, onChange, disabled }) {
    const { documents, selectedDocumentNum, requestedDocumentNum, isLoading, error, reload, source } = selection;
    const inputId = `review-document-${config.value}`;
    const ready = Boolean(source.document);

    return (
        <section className="review-document-selector" aria-labelledby={`${inputId}-title`}>
            <div className="review-selector-heading">
                <label id={`${inputId}-title`} htmlFor={inputId}>
                    {config.label}{' '}
                    <span className={`review-requirement ${config.required ? 'is-required' : ''}`}>
                        {config.required ? '필수' : '선택'}
                    </span>
                </label>
                <Link to={`/documents/write?type=${config.value}`}>
                    {config.value === 'portfolio' ? 'PDF 등록' : '새 문서 작성'}{' '}
                    <ArrowRight size={14} aria-hidden="true" />
                </Link>
            </div>
            <p className="review-document-hint">{config.hint}</p>
            <select
                id={inputId}
                value={selectedDocumentNum}
                onChange={(event) => onChange(event.target.value)}
                disabled={disabled || isLoading || Boolean(error) || documents.length === 0}
                required={config.required}
                aria-required={config.required}
            >
                <option value="">{config.required ? `${config.label}를 선택해 주세요.` : '첨부하지 않음'}</option>
                {documents.map((document) => (
                    <option key={document.documentNum} value={document.documentNum}>
                        {document.documentTitle || '제목 없음'}
                    </option>
                ))}
            </select>
            {isLoading ? (
                <p className="review-status" role="status">
                    <LoaderCircle size={17} className="review-spinner" aria-hidden="true" /> 문서 목록을 불러오고
                    있습니다.
                </p>
            ) : error ? (
                <div className="review-status is-error" role="alert">
                    <p>{error}</p>
                    <button className="review-text-button" type="button" onClick={reload} disabled={disabled}>
                        <RotateCcw size={14} aria-hidden="true" /> 목록 다시 시도
                    </button>
                </div>
            ) : documents.length === 0 ? (
                <div className="review-no-documents">
                    <FileText size={23} aria-hidden="true" />
                    <p>등록된 {config.label}가 없습니다.</p>
                    <Link className="review-secondary-button" to={`/documents/write?type=${config.value}`}>
                        {config.label} {config.required ? '작성하기' : '등록하기'}
                    </Link>
                </div>
            ) : requestedDocumentNum && !selectedDocumentNum ? (
                <p className="review-field-error" role="alert">
                    선택한 문서를 목록에서 찾을 수 없습니다. 다시 선택해 주세요.
                </p>
            ) : null}
            {selectedDocumentNum && (
                <DocumentSourcePreview
                    documentType={config.value}
                    documentLabel={config.label}
                    documentNum={selectedDocumentNum}
                    {...source}
                    onRetry={source.reload}
                />
            )}
            <div className="review-selection-state" aria-live="polite">
                <span className={ready ? 'is-ready' : ''}>
                    {ready && <CheckCircle2 size={14} aria-hidden="true" />}
                    {ready
                        ? '첨삭 자료에 포함됩니다.'
                        : config.required
                          ? '첨삭에 필요한 문서를 선택해 주세요.'
                          : '포트폴리오 없이도 통합 첨삭할 수 있습니다.'}
                </span>
                {!config.required && requestedDocumentNum && (
                    <button
                        type="button"
                        className="review-text-button"
                        onClick={() => onChange('')}
                        disabled={disabled}
                    >
                        포트폴리오 선택 해제
                    </button>
                )}
            </div>
        </section>
    );
}

export default ReviewDocumentSelector;
