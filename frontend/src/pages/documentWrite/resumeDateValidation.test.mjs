import assert from 'node:assert/strict';
import { test } from 'node:test';
import { getResumeDateError, getTodayInSeoul } from './resumeDateValidation.js';

test('한국 자정에 오늘 날짜가 바뀐다', () => {
    assert.equal(getTodayInSeoul(new Date('2026-09-30T14:59:59Z')), '2026-09-30');
    assert.equal(getTodayInSeoul(new Date('2026-09-30T15:00:00Z')), '2026-10-01');
});

test('입학일, 입사일, 취득일의 미래 날짜만 거부한다', () => {
    for (const [field, label] of Object.entries({
        admissionDate: '입학일', joinDate: '입사일', issueDate: '취득일',
    })) {
        assert.equal(getResumeDateError(field, '2026-10-02', '2026-10-01'),
            `${label}은 오늘 이후 날짜를 선택할 수 없습니다.`);
        for (const value of ['2026-10-01', '2026-09-30', '', null]) {
            assert.equal(getResumeDateError(field, value, '2026-10-01'), '');
        }
    }
});

test('연도가 바뀌어도 미래 날짜를 거부하고 예정 종료일은 허용한다', () => {
    assert.notEqual(getResumeDateError('admissionDate', '2027-01-01', '2026-12-31'), '');
    assert.equal(getResumeDateError('graduationDate', '2027-01-01', '2026-12-31'), '');
    assert.equal(getResumeDateError('resignDate', '2027-01-01', '2026-12-31'), '');
});
