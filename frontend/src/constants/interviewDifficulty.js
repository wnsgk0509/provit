export const INTERVIEW_DIFFICULTIES = [
    { value: 'EASY', label: '일반면접', description: '기초 개념과 본인의 역할을 확인하고, 단순한 상황에서 기본 대응을 묻습니다.' },
    { value: 'NORMAL', label: '심층면접', description: '선택한 이유와 판단 근거, 성과를 확인한 방법까지 깊이 검증합니다.' },
    { value: 'HARD', label: '압박면접', description: '반론·한계와 충돌하는 제약을 제시해, 기존 판단을 지킬 근거나 수정할 기준을 묻습니다.' },
];

export const DIFFICULTY_NAMES = Object.fromEntries(
    INTERVIEW_DIFFICULTIES.map(({ value, label }) => [value, label]),
);
