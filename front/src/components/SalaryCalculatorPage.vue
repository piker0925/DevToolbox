<template>
  <div class="flex flex-col gap-4">
    <div class="flex gap-1 overflow-x-auto rounded-lg border border-border bg-card p-1">
      <button
          v-for="t in TABS"
          :key="t.id"
          :class="tab === t.id
          ? 'bg-primary text-primary-foreground'
          : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'"
          class="shrink-0 whitespace-nowrap rounded-md px-3 py-1.5 text-[13px] font-medium transition-colors sm:flex-1"
          @click="tab = t.id"
      >{{ t.label }}
      </button>
    </div>

    <div class="rounded-xl border border-border bg-card p-4">
      <div v-if="tab === 'net-pay'" data-testid="tab-panel-net-pay" class="flex flex-col gap-3">
        <label class="flex flex-col gap-1.5 text-[13px]">
          연봉(세전, 만원)
          <input v-model="annualSalaryManwonInput" type="text" inputmode="numeric" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <label class="flex flex-col gap-1.5 text-[13px]">
          공제대상가족수(본인 포함)
          <input v-model.number="dependents" type="number" inputmode="numeric" min="1" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <label class="flex flex-col gap-1.5 text-[13px]">
          8~20세 자녀 수 (공제대상가족수와 별개로 추가 세액공제)
          <input v-model.number="childrenAged8to20" type="number" inputmode="numeric" min="0" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <label class="flex flex-col gap-1.5 text-[13px]">
          월 비과세 소득(식대·자가운전보조금 등, 만원, 없으면 0)
          <input v-model="nonTaxableManwonInput" type="text" inputmode="numeric" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <div class="rounded-lg border border-zone-accent-life/20 bg-zone-accent-life/10 px-4 py-4 text-center">
          <div class="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">월 실수령액(추정)</div>
          <div class="mt-1 font-mono text-2xl font-semibold text-zone-accent-life">{{ netPay.netPay.toLocaleString() }}원</div>
          <div v-if="nonTaxableManwon > 0" class="mt-1 text-[11px] text-muted-foreground">비과세 소득 {{ (nonTaxableManwon * MANWON).toLocaleString() }}원은 과세 계산에서 제외하고 그대로 더한 값입니다</div>
        </div>
        <div class="divide-y divide-border rounded-lg border border-border">
          <div v-for="row in netPayBreakdown" :key="row.label" class="flex items-center justify-between px-3 py-2 text-[13px]">
            <span class="text-muted-foreground">{{ row.label }}</span>
            <span class="font-mono text-foreground">{{ row.value.toLocaleString() }}원</span>
          </div>
        </div>
        <p class="text-[11px] text-muted-foreground/70">국세청 근로소득 간이세액표(2026.03.01 시행분, 소득세법 시행령 별표2) 기준</p>
      </div>

      <div v-else-if="tab === 'hourly-monthly'" data-testid="tab-panel-hourly-monthly" class="flex flex-col gap-3">
        <label class="flex flex-col gap-1.5 text-[13px]">
          기준
          <select v-model="salaryUnit" class="rounded-md border border-input bg-background px-3 py-2">
            <option value="hourly">시급 입력</option>
            <option value="monthly">월급 입력</option>
            <option value="annual">연봉 입력</option>
          </select>
        </label>
        <label class="flex flex-col gap-1.5 text-[13px]">
          {{ SALARY_UNIT_LABEL[salaryUnit] }}({{ salaryUnit === 'hourly' ? '원' : '만원' }})
          <input v-model="salaryInputFormatted" type="text" inputmode="numeric" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <label class="flex flex-col gap-1.5 text-[13px]">
          월 소정근로시간
          <input v-model.number="monthlyHours" type="number" inputmode="numeric" min="1" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <p class="text-[11px] text-muted-foreground/70">기본값 209시간은 주 40시간 근무제의 법정 통상임금 산정 기준시간(유급주휴 포함)입니다 — 평균이 아니라 표준값이며, 계약 근로시간이 다르면 바꿔주세요.</p>
        <div v-if="belowMinimumWage" class="text-[13px] text-destructive">2026년 최저임금({{ MINIMUM_WAGE_2026_HOURLY.toLocaleString() }}원) 미만입니다.</div>
        <div class="grid grid-cols-1 gap-2 sm:grid-cols-3 sm:gap-3">
          <div
              v-for="s in [
                {label: '시급', value: hourlyValue},
                {label: `월급(${monthlyHours}시간 기준)`, value: monthlyValue},
                {label: '연봉', value: annualValue},
              ]"
              :key="s.label"
              class="flex flex-col items-center gap-1 rounded-lg border border-zone-accent-life/20 bg-zone-accent-life/10 py-3"
          >
            <span class="font-mono text-base font-semibold text-zone-accent-life">{{ s.value.toLocaleString() }}원</span>
            <span class="text-[11px] text-muted-foreground">{{ s.label }}</span>
          </div>
        </div>
      </div>

      <div v-else-if="tab === 'severance'" data-testid="tab-panel-severance" class="flex flex-col gap-3">
        <label class="flex flex-col gap-1.5 text-[13px]">
          입사일
          <input v-model="hireDate" type="date" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <label class="flex flex-col gap-1.5 text-[13px]">
          퇴사일(예정일도 가능)
          <input v-model="leaveDate" type="date" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <div v-if="dateRangeInvalid" class="text-[13px] text-destructive">퇴사일이 입사일보다 빠릅니다.</div>
        <label class="flex flex-col gap-1.5 text-[13px]">
          퇴직 전 3개월 임금총액(만원)
          <input v-model="threeMonthWageManwonInput" type="text" inputmode="numeric" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <p class="text-[11px] text-muted-foreground/70">최근 3개월 급여명세서의 세전 총액(기본급+제수당)을 더한 값이에요. 재직일수와 3개월 산정기간 일수는 입사일·퇴사일로 자동 계산됩니다.</p>

        <details class="rounded-lg border border-border px-3 py-2 text-[13px]">
          <summary class="cursor-pointer select-none text-muted-foreground">상여금·연차수당이 있나요? (선택 입력, 더 정확해집니다)</summary>
          <div class="mt-3 flex flex-col gap-3">
            <label class="flex flex-col gap-1.5">
              최근 1년간 지급된 상여금 총액(만원)
              <input v-model="annualBonusManwonInput" type="text" inputmode="numeric" class="rounded-md border border-input bg-background px-3 py-2"/>
            </label>
            <label class="flex flex-col gap-1.5">
              최근 1년간 지급된 연차수당(만원)
              <input v-model="annualLeaveAllowanceManwonInput" type="text" inputmode="numeric" class="rounded-md border border-input bg-background px-3 py-2"/>
            </label>
          </div>
        </details>

        <div class="rounded-lg border border-zone-accent-life/20 bg-zone-accent-life/10 px-4 py-4 text-center">
          <div class="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">예상 퇴직금</div>
          <div class="mt-1 font-mono text-2xl font-semibold text-zone-accent-life">{{ severancePay.toLocaleString() }}원</div>
        </div>
        <div class="divide-y divide-border rounded-lg border border-border">
          <div class="flex items-center justify-between px-3 py-2 text-[13px]">
            <span class="text-muted-foreground">재직일수</span>
            <span class="font-mono text-foreground">{{ tenureDays }}일</span>
          </div>
          <div class="flex items-center justify-between px-3 py-2 text-[13px]">
            <span class="text-muted-foreground">1일 평균임금(상여금·연차수당 3/12 안분 반영)</span>
            <span class="font-mono text-foreground">{{ averageDailyWage.toLocaleString() }}원</span>
          </div>
        </div>
        <p class="text-[11px] text-muted-foreground/70">근로기준법 §2 평균임금 · 근로자퇴직급여 보장법 §8 기준(30일분 × 근속연수). 통상임금과의 최저한도 비교는 포함하지 않은 단순 모델이라 실제 수령액과 다를 수 있습니다 — 정확한 금액은 회사 인사팀 또는 고용노동부에 확인하세요.</p>
      </div>

      <div v-else-if="tab === 'work-hours'" data-testid="tab-panel-work-hours" class="flex flex-col gap-3">
        <p class="text-[11px] text-muted-foreground/70">시급제·주급제 근로자가 주 40시간을 초과해 일했을 때 가산수당을 계산합니다. 월급제라면 "시급↔월급↔연봉" 탭에서 환산시급을 먼저 구해 아래에 입력하세요.</p>
        <label class="flex flex-col gap-1.5 text-[13px]">
          시급(원)
          <input v-model="workHourlyWageInput" type="text" inputmode="numeric" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <label class="flex flex-col gap-1.5 text-[13px]">
          주간 근무시간
          <input v-model.number="weeklyHours" type="number" inputmode="numeric" min="0" class="rounded-md border border-input bg-background px-3 py-2"/>
        </label>
        <div class="rounded-lg border border-zone-accent-life/20 bg-zone-accent-life/10 px-4 py-4 text-center">
          <div class="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">초과근무수당(1.5배 가산)</div>
          <div class="mt-1 font-mono text-2xl font-semibold text-zone-accent-life">{{ overtimePayAmount.toLocaleString() }}원</div>
        </div>
        <div class="flex items-center justify-between rounded-lg border border-border px-3 py-2 text-[13px]">
          <span class="text-muted-foreground">초과근무시간(주 40시간 기준)</span>
          <span class="font-mono text-foreground">{{ overtimeHours }}시간</span>
        </div>
        <p class="text-[11px] text-muted-foreground/70">근로기준법 §56 연장근로 가산(통상임금의 50% 이상)</p>
      </div>
    </div>

    <p class="text-[11px] text-muted-foreground/70">참고용 계산이며 법적 효력이 없습니다 · 2026년 기준</p>
  </div>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {
  annualToMonthly,
  calcAverageDailyWage,
  calcMonthlyNetPayWithNonTaxable,
  calcSeverancePay,
  daysBetweenInclusive,
  hourlyToMonthly,
  isBelowMinimumWage2026,
  monthlyToAnnual,
  monthlyToHourly,
  overtimePay,
  threeMonthPeriodDays,
  weeklyOvertimeHours,
} from '../utils/salaryCalc'
import {MINIMUM_WAGE_2026_HOURLY} from '../utils/salaryRates2026'
import {useCommaNumberInput} from '../utils/commaNumberInput'

type TabId = 'net-pay' | 'hourly-monthly' | 'severance' | 'work-hours'

const TABS: {id: TabId; label: string}[] = [
  {id: 'net-pay', label: '연봉 실수령액'},
  {id: 'hourly-monthly', label: '시급↔월급↔연봉'},
  {id: 'severance', label: '퇴직금'},
  {id: 'work-hours', label: '초과근무수당'},
]

const MANWON = 10_000

const route = useRoute()
const router = useRouter()

const initialTab = typeof route.query.tab === 'string' && TABS.some(t => t.id === route.query.tab)
    ? route.query.tab as TabId
    : TABS[0].id

const tab = ref<TabId>(initialTab)

watch(tab, id => {
  if (route.query.tab === id) return
  router.replace({query: {...route.query, tab: id}})
})
watch(() => route.query.tab, q => {
  if (typeof q === 'string' && q !== tab.value && TABS.some(t => t.id === q)) tab.value = q as TabId
})

// 연봉 실수령액 — 큰 금액은 만원 단위로 입력받고 계산 시점에 원으로 환산한다
const annualSalaryManwon = ref(3_000)
const annualSalaryManwonInput = useCommaNumberInput(annualSalaryManwon)
const dependents = ref(1)
const childrenAged8to20 = ref(0)
const nonTaxableManwon = ref(0)
const nonTaxableManwonInput = useCommaNumberInput(nonTaxableManwon)
const netPay = computed(() => calcMonthlyNetPayWithNonTaxable(
    annualToMonthly(annualSalaryManwon.value * MANWON),
    nonTaxableManwon.value * MANWON,
    dependents.value,
    childrenAged8to20.value,
))
const netPayBreakdown = computed(() => [
  {label: '국민연금', value: netPay.value.nationalPension},
  {label: '건강보험', value: netPay.value.healthInsurance},
  {label: '장기요양보험', value: netPay.value.longTermCare},
  {label: '고용보험', value: netPay.value.employmentInsurance},
  {label: '소득세', value: netPay.value.incomeTax},
  {label: '지방소득세', value: netPay.value.localIncomeTax},
])

// 시급 ↔ 월급 ↔ 연봉 — 세 단위 중 하나를 기준으로 선택해 입력하면 나머지 두 값을 역산한다
// 시급은 만원 단위로 쓰기엔 너무 작은 금액이라 원 단위를 유지하고, 월급·연봉만 만원 단위로 입력받는다
type SalaryUnit = 'hourly' | 'monthly' | 'annual'
const SALARY_UNIT_LABEL: Record<SalaryUnit, string> = {hourly: '시급', monthly: '월급', annual: '연봉'}
const SALARY_UNIT_DEFAULT: Record<SalaryUnit, number> = {hourly: MINIMUM_WAGE_2026_HOURLY, monthly: 216, annual: 3_000}
const salaryUnit = ref<SalaryUnit>('hourly')
const salaryInput = ref(SALARY_UNIT_DEFAULT.hourly)
const salaryInputFormatted = useCommaNumberInput(salaryInput)
const monthlyHours = ref(209)

watch(salaryUnit, unit => {
  salaryInput.value = SALARY_UNIT_DEFAULT[unit]
})

const salaryInputWon = computed(() => salaryUnit.value === 'hourly' ? salaryInput.value : salaryInput.value * MANWON)

const hourlyValue = computed(() => {
  if (salaryUnit.value === 'hourly') return salaryInputWon.value
  if (salaryUnit.value === 'monthly') return monthlyToHourly(salaryInputWon.value, monthlyHours.value)
  return monthlyToHourly(annualToMonthly(salaryInputWon.value), monthlyHours.value)
})
const monthlyValue = computed(() => {
  if (salaryUnit.value === 'monthly') return salaryInputWon.value
  if (salaryUnit.value === 'hourly') return hourlyToMonthly(salaryInputWon.value, monthlyHours.value)
  return annualToMonthly(salaryInputWon.value)
})
const annualValue = computed(() => {
  if (salaryUnit.value === 'annual') return salaryInputWon.value
  if (salaryUnit.value === 'monthly') return monthlyToAnnual(salaryInputWon.value)
  return monthlyToAnnual(hourlyToMonthly(salaryInputWon.value, monthlyHours.value))
})
const belowMinimumWage = computed(() => salaryUnit.value === 'hourly' && isBelowMinimumWage2026(salaryInputWon.value))

// 퇴직금 — 재직일수·3개월 산정기간 일수는 사용자가 직접 세지 않고 입사일·퇴사일로 자동 계산한다
function formatDate(d: Date): string {
  return d.toISOString().slice(0, 10)
}
const todayForDefault = new Date()
const leaveDate = ref(formatDate(todayForDefault))
const hireDateDefault = new Date(todayForDefault)
hireDateDefault.setFullYear(hireDateDefault.getFullYear() - 1)
const hireDate = ref(formatDate(hireDateDefault))

const dateRangeInvalid = computed(() => !!hireDate.value && !!leaveDate.value && new Date(leaveDate.value) < new Date(hireDate.value))
const tenureDays = computed(() => {
  if (!hireDate.value || !leaveDate.value || dateRangeInvalid.value) return 0
  return daysBetweenInclusive(hireDate.value, leaveDate.value)
})
const threeMonthDays = computed(() => {
  if (!leaveDate.value) return 1
  const days = threeMonthPeriodDays(leaveDate.value)
  return days > 0 ? days : 1
})

const threeMonthWageManwon = ref(900)
const threeMonthWageManwonInput = useCommaNumberInput(threeMonthWageManwon)
const annualBonusManwon = ref(0)
const annualBonusManwonInput = useCommaNumberInput(annualBonusManwon)
const annualLeaveAllowanceManwon = ref(0)
const annualLeaveAllowanceManwonInput = useCommaNumberInput(annualLeaveAllowanceManwon)
const averageDailyWage = computed(() => calcAverageDailyWage(
    threeMonthWageManwon.value * MANWON,
    threeMonthDays.value,
    annualBonusManwon.value * MANWON,
    annualLeaveAllowanceManwon.value * MANWON,
))
const severancePay = computed(() => calcSeverancePay(averageDailyWage.value, tenureDays.value))

// 초과근무수당 — 시급은 작은 금액이라 원 단위 유지하되 콤마 포맷은 그대로 적용
const workHourlyWage = ref(10000)
const workHourlyWageInput = useCommaNumberInput(workHourlyWage)
const weeklyHours = ref(40)
const overtimeHours = computed(() => weeklyOvertimeHours(weeklyHours.value))
const overtimePayAmount = computed(() => overtimePay(workHourlyWage.value, weeklyHours.value))
</script>
