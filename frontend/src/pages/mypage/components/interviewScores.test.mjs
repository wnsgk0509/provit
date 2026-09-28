import test from 'node:test';
import assert from 'node:assert/strict';
import { radarPoint, radarPolygon, sortInterviewResults, formatScore } from './interviewScores.js';

test('최초와 최근 면접은 날짜와 동일 날짜의 기록 번호로 결정한다', () => {
    const input = [
        { historyNum: 12, interviewDate: '2026-09-27 09:00:00' },
        { historyNum: 11, interviewDate: '2026-09-26 09:00:00' },
        { historyNum: 13, interviewDate: '2026-09-27 09:00:00' },
    ];
    const sorted = sortInterviewResults(input);
    assert.deepEqual(sorted.map((item) => item.historyNum), [13, 12, 11]);
    assert.deepEqual(input.map((item) => item.historyNum), [12, 11, 13]);
});

test('기록이 없거나 하나인 경우에도 정렬이 안전하다', () => {
    assert.deepEqual(sortInterviewResults([]), []);
    assert.deepEqual(sortInterviewResults([{ historyNum: 1 }]), [{ historyNum: 1 }]);
});

test('오각형 점수는 100점 척도를 사용하고 범위를 벗어나지 않는다', () => {
    assert.deepEqual(radarPoint(0, 100), [200, 70]);
    assert.deepEqual(radarPoint(0, 50), [200, 125]);
    assert.deepEqual(radarPoint(0, -10), [200, 180]);
    assert.deepEqual(radarPoint(0, 150), [200, 70]);
    assert.deepEqual(radarPoint(0, 'invalid'), [200, 180]);
    assert.equal(radarPolygon({ documentConsistencyScore: 100 }).split(' ').length, 5);
    assert.equal(radarPolygon({}).includes('NaN'), false);
});

test('점수 0과 소수는 표시하고 누락된 점수는 구분한다', () => {
    assert.equal(formatScore(0), '0');
    assert.equal(formatScore(81.25), '81.3');
    assert.equal(formatScore(undefined), '-');
    assert.equal(formatScore(null), '-');
});
