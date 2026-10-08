const pastDateFields = {
    admissionDate: '입학일',
    joinDate: '입사일',
    issueDate: '취득일',
};

export function getTodayInSeoul(now = new Date()) {
    return new Intl.DateTimeFormat('sv-SE', {
        timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
    }).format(now);
}

export function getResumeDateError(field, value, today = getTodayInSeoul()) {
    return pastDateFields[field] && value && value > today
        ? `${pastDateFields[field]}은 오늘 이후 날짜를 선택할 수 없습니다.`
        : '';
}
