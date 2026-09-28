import { useEffect, useState } from 'react';
import { getReviewDocument, getReviewDocuments } from '../../api/documentReviewApi';
import { reviewErrorMessage } from './documentReviewConfig';

export function useReviewDocuments(documentType) {
    const [revision, setRevision] = useState(0);
    const [resource, setResource] = useState(null);
    const resourceKey = `${documentType}:${revision}`;

    useEffect(() => {
        let active = true;
        getReviewDocuments(documentType).then(
            (data) => {
                if (active) setResource({ key: resourceKey, data });
            },
            (error) => {
                if (active)
                    setResource({
                        key: resourceKey,
                        error: reviewErrorMessage(error, '문서 목록을 불러오지 못했습니다.'),
                    });
            },
        );
        return () => {
            active = false;
        };
    }, [documentType, resourceKey]);

    const current = resource?.key === resourceKey ? resource : null;
    return {
        documents: current?.data || [],
        isLoading: !current,
        error: current?.error || '',
        reload: () => setRevision((value) => value + 1),
    };
}

export function useReviewDocument(documentType, documentNum) {
    const [revision, setRevision] = useState(0);
    const [resource, setResource] = useState(null);
    const resourceKey = `${documentType}:${documentNum}:${revision}`;

    useEffect(() => {
        if (!documentNum) return undefined;
        let active = true;
        getReviewDocument(documentType, documentNum).then(
            (data) => {
                if (active)
                    setResource(
                        data
                            ? { key: resourceKey, data }
                            : { key: resourceKey, error: '선택한 문서의 내용을 불러오지 못했습니다.' },
                    );
            },
            (error) => {
                if (active)
                    setResource({ key: resourceKey, error: reviewErrorMessage(error, '문서를 불러오지 못했습니다.') });
            },
        );
        return () => {
            active = false;
        };
    }, [documentType, documentNum, resourceKey]);

    const current = resource?.key === resourceKey ? resource : null;
    return {
        document: current?.data || null,
        isLoading: Boolean(documentNum) && !current,
        error: current?.error || '',
        reload: () => setRevision((value) => value + 1),
    };
}

export function useReviewSelection(documentType, requestedDocumentNum) {
    const list = useReviewDocuments(documentType);
    const selectedDocumentNum = list.documents.some((document) => String(document.documentNum) === requestedDocumentNum)
        ? requestedDocumentNum
        : '';
    const source = useReviewDocument(documentType, selectedDocumentNum);
    return { ...list, selectedDocumentNum, requestedDocumentNum, source };
}
