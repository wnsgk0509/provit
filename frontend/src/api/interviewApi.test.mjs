import assert from 'node:assert/strict';
import { mock, test } from 'node:test';
import client from './client.js';
import { createInterview } from './interviewApi.js';

const settings = {
    resumeNum: '1',
    portfolioNum: '',
    letterNum: '2',
    difficulty: 'NORMAL',
};

test('공고에서 시작한 면접은 회사·직무·경력 정보를 시작 요청에 전달한다', async () => {
    const recruitment = {
        recruitmentNum: 42,
        companyName: '테스트 기업',
        title: '콘텐츠마케터 채용',
        jobName: '콘텐츠마케팅,SNS마케팅',
        locationName: '서울',
        experienceLevel: '신입',
    };
    const session = { historyNum: 17, questions: [] };
    const post = mock.method(client, 'post', async (url, body, options) => {
        assert.equal(url, '/interview/start');
        assert.deepEqual(body.recruitment, recruitment);
        assert.equal(body.resumeNum, 1);
        assert.equal(body.letterNum, 2);
        assert.equal(body.portfolioNum, 0);
        assert.equal(body.requestId, 'request-id');
        assert.equal(body.interviewDifficulty, 'NORMAL');
        assert.deepEqual(Object.keys(body).sort(), ['resumeNum', 'portfolioNum', 'letterNum',
            'interviewDifficulty', 'requestId', 'recruitment'].sort());
        assert.equal(options.timeout, 150000);
        return { data: { data: session } };
    });
    try {
        assert.equal(await createInterview(settings, 'request-id', recruitment), session);
    } finally {
        post.mock.restore();
    }
});

test('공고 선택 없이 시작하는 기존 면접은 공고 정보를 요구하지 않는다', async () => {
    const post = mock.method(client, 'post', async (_url, body) => {
        assert.equal(body.recruitment, null);
        return { data: { data: { historyNum: 18 } } };
    });
    try {
        assert.deepEqual(await createInterview(settings, 'request-id'), { historyNum: 18 });
    } finally {
        post.mock.restore();
    }
});
