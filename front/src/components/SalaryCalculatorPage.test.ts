import {describe, expect, it} from 'vitest'
import {mount} from '@vue/test-utils'
import {createMemoryHistory, createRouter} from 'vue-router'
import SalaryCalculatorPage from './SalaryCalculatorPage.vue'

async function mountWithQuery(query: string) {
    const router = createRouter({
        history: createMemoryHistory(),
        routes: [{path: '/tools/salary-calculator', component: SalaryCalculatorPage}],
    })
    router.push('/tools/salary-calculator' + query)
    await router.isReady()
    return mount(SalaryCalculatorPage, {global: {plugins: [router]}})
}

describe('SalaryCalculatorPage', () => {
    it('쿼리 없이 진입하면 기본 탭(연봉 실수령액)이 열림', async () => {
        const wrapper = await mountWithQuery('')
        expect(wrapper.text()).toContain('연봉 실수령액')
        expect(wrapper.find('[data-testid="tab-panel-net-pay"]').exists()).toBe(true)
        expect(wrapper.find('[data-testid="tab-panel-work-hours"]').exists()).toBe(false)
    })

    it('?tab=work-hours 쿼리로 진입하면 근무시간 탭이 바로 열림(딥링크)', async () => {
        const wrapper = await mountWithQuery('?tab=work-hours')
        expect(wrapper.find('[data-testid="tab-panel-work-hours"]').exists()).toBe(true)
        expect(wrapper.find('[data-testid="tab-panel-net-pay"]').exists()).toBe(false)
    })

    it('탭과 무관하게 면책 문구와 기준연도가 항상 보임', async () => {
        const wrapper = await mountWithQuery('?tab=severance')
        expect(wrapper.text()).toContain('참고용 계산이며 법적 효력이 없습니다')
        expect(wrapper.text()).toContain('2026년 기준')
    })

    it('시급↔월급↔연봉 탭에서 기준을 월급으로 바꾸면 입력한 월급(만원 단위, 콤마 포맷 텍스트 입력) 기준으로 시급·연봉이 역산됨(단방향 아님)', async () => {
        const wrapper = await mountWithQuery('?tab=hourly-monthly')
        const panel = wrapper.find('[data-testid="tab-panel-hourly-monthly"]')
        await panel.find('select').setValue('monthly')
        await panel.findAll('input')[0].setValue('250') // 250만원 = 2,500,000원
        expect(wrapper.text()).toContain('11,962원') // 2,500,000 / 209시간 반올림
        expect(wrapper.text()).toContain('30,000,000원') // 2,500,000 × 12
    })

    it('기준을 월급으로 두면 최저임금 미만 경고는 뜨지 않음(경고는 시급 기준 입력에서만 유효)', async () => {
        const wrapper = await mountWithQuery('?tab=hourly-monthly')
        const panel = wrapper.find('[data-testid="tab-panel-hourly-monthly"]')
        await panel.find('select').setValue('monthly')
        await panel.findAll('input')[0].setValue('50')
        expect(wrapper.text()).not.toContain('최저임금')
    })

    it('월 소정근로시간을 기본값(209) 대신 174시간(단시간 근로 계약)으로 바꾸면 시급 환산이 그에 맞게 달라짐', async () => {
        const wrapper = await mountWithQuery('?tab=hourly-monthly')
        const panel = wrapper.find('[data-testid="tab-panel-hourly-monthly"]')
        await panel.find('select').setValue('monthly')
        const inputs = panel.findAll('input')
        await inputs[0].setValue('200') // 200만원 = 2,000,000원
        await inputs[1].setValue(174) // 월 소정근로시간(개수 필드라 콤마 포맷 미적용, type=number 그대로)
        expect(wrapper.text()).toContain('11,494원') // 2,000,000 / 174시간 반올림
    })

    it('시급 입력칸에 콤마가 포함된 숫자를 입력해도 정상 파싱됨(예: "10,320")', async () => {
        const wrapper = await mountWithQuery('?tab=hourly-monthly')
        const panel = wrapper.find('[data-testid="tab-panel-hourly-monthly"]')
        await panel.findAll('input')[0].setValue('10,320')
        expect(wrapper.text()).toContain('2,156,880원') // 10,320 × 209시간
    })

    it('퇴직금 탭에서 입사일·퇴사일을 입력하면 재직일수가 자동 계산됨(직접 일수를 세지 않아도 됨)', async () => {
        const wrapper = await mountWithQuery('?tab=severance')
        const panel = wrapper.find('[data-testid="tab-panel-severance"]')
        const dateInputs = panel.findAll('input[type="date"]')
        await dateInputs[0].setValue('2025-01-01')
        await dateInputs[1].setValue('2025-12-31')
        expect(wrapper.text()).toContain('365일')
    })

    it('퇴직금 탭에서 퇴사일이 입사일보다 빠르면 경고가 뜨고 재직일수가 0으로 안전하게 처리됨', async () => {
        const wrapper = await mountWithQuery('?tab=severance')
        const panel = wrapper.find('[data-testid="tab-panel-severance"]')
        const dateInputs = panel.findAll('input[type="date"]')
        await dateInputs[0].setValue('2025-12-31')
        await dateInputs[1].setValue('2025-01-01')
        expect(wrapper.text()).toContain('퇴사일이 입사일보다 빠릅니다')
        expect(wrapper.text()).toContain('0일')
    })

    it('퇴직금 탭의 상여금·연차수당 선택 입력은 기본적으로 접혀 있음(불필요한 필드로 화면을 채우지 않음)', async () => {
        const wrapper = await mountWithQuery('?tab=severance')
        const details = wrapper.find('[data-testid="tab-panel-severance"] details')
        expect(details.attributes('open')).toBeUndefined()
    })
})
