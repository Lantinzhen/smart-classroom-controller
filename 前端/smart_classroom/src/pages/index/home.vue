<template>
  <view class="container">
    <!-- 顶部卡片 -->
    <view class="card security-card">
      <view class="card-header">
        <text class="card-title">智能安全</text>
        <text class="more">●</text>
      </view>
      <view class="card-content">
        <image src="/static/security-illustration.png" mode="aspectFit" class="illustration"></image>
        <text class="subtitle">管理家庭安全</text>
      </view>
    </view>

    <!-- 控制面板 -->
    <view class="control-grid">
      <!-- 门锁控制 -->
      <view class="control-item" :class="{ active: doorLocked }" @click="toggleDoor">
        <view class="control-icon">🚪</view>
        <text class="control-label">门锁</text>
        <text class="control-status">{{ doorLocked ? '关闭' : '开启' }}</text>
      </view>

      <!-- 安全报警 -->
      <view class="control-item" :class="{ active: securityAlert }" @click="toggleSecurity">
        <view class="control-icon">🛡️</view>
        <text class="control-label">安全报警</text>
        <text class="control-status">{{ securityAlert ? '开启' : '关闭' }}</text>
      </view>

      <!-- 风扇控制 -->
      <view class="control-item" :class="{ active: fanOn }" @click="toggleFan">
        <view class="control-icon">💨</view>
        <text class="control-label">风扇</text>
        <text class="control-status">{{ fanOn ? '开启' : '关闭' }}</text>
      </view>

      <!-- 光照显示 -->
      <view class="control-item">
        <view class="control-icon">☀️</view>
        <text class="control-label">光照</text>
        <text class="control-status">{{ lightLevel }} lux</text>
      </view>

      <!-- 故障警报 -->
      <view class="control-item warning" v-if="hasError">
        <view class="control-icon">⚠️</view>
        <text class="control-label">故障警报</text>
        <text class="control-status">{{ errorMessage }}</text>
      </view>

      <!-- 烟雾探测 -->
      <view class="control-item" :class="{ warning: smokeLevel > 0.1 }">
        <view class="control-icon">🌫️</view>
        <text class="control-label">烟雾探测</text>
        <text class="control-status">{{ smokeLevel }}%</text>
      </view>
    </view>

    <!-- 环境监测 -->
    <view class="environment-section">
      <view class="env-card" @click="showTempChart = true">
        <text class="env-title">温度</text>
        <text class="env-value">{{ temperature }}°C</text>
        <text class="env-tip">点击查看趋势</text>
      </view>
      <view class="env-card" @click="showHumidityChart = true">
        <text class="env-title">湿度</text>
        <text class="env-value">{{ humidity }}%</text>
        <text class="env-tip">点击查看趋势</text>
      </view>
    </view>

    <!-- 温度趋势图弹窗 -->
    <view class="modal" v-if="showTempChart" @click="showTempChart = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">温度变化趋势</text>
          <view class="modal-actions">
            <text class="modal-action-btn" @click="goToAiAnalysis('temperature')">AI分析</text>
            <text class="modal-close" @click="showTempChart = false">×</text>
          </view>
        </view>
        <view class="chart-container">
          <canvas canvas-id="tempChart" id="tempChart" class="chart"></canvas>
        </view>
      </view>
    </view>

    <!-- 湿度趋势图弹窗 -->
    <view class="modal" v-if="showHumidityChart" @click="showHumidityChart = false">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <text class="modal-title">湿度变化趋势</text>
          <view class="modal-actions">
            <text class="modal-action-btn" @click="goToAiAnalysis('humidity')">AI分析</text>
            <text class="modal-close" @click="showHumidityChart = false">×</text>
          </view>
        </view>
        <view class="chart-container">
          <canvas canvas-id="humidityChart" id="humidityChart" class="chart"></canvas>
        </view>
      </view>
    </view>

    <!-- 底部导航栏 -->
    <view class="tab-bar">
      <view class="tab-item" :class="{ active: currentTab === 'monitor' }" @click="switchTab('monitor')">
        <text class="tab-icon">📺</text>
        <text class="tab-text">监控</text>
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'ai' }" @click="switchTab('ai')">
        <text class="tab-icon">🤖</text>
        <text class="tab-text">AI</text>
      </view>
      <view class="tab-item" :class="{ active: currentTab === 'person' }" @click="switchTab('person')">
        <text class="tab-icon">👤</text>
        <text class="tab-text">我的</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import uCharts from '@/utils/u-charts.min.js'

// 状态变量
const doorLocked = ref(true)
const securityAlert = ref(false)
const hasError = ref(false)
const errorMessage = ref('')
const smokeLevel = ref(0)
const temperature = ref(25)
const humidity = ref(60)
const currentTab = ref('monitor')
const fanOn = ref(false)
const lightLevel = ref(0)
const showTempChart = ref(false)
const showHumidityChart = ref(false)

// 历史数据
const tempHistory = ref([])
const humidityHistory = ref([])

// WebSocket连接
let ws = null

let tempChart = null
let humidityChart = null

// 初始化WebSocket连接
const initWebSocket = () => {
  ws = uni.connectSocket({
    url: 'ws://your-backend-url/ws',
    success: () => {
      console.log('WebSocket连接成功')
    }
  })

  ws.onMessage((res) => {
    const data = JSON.parse(res.data)
    handleWebSocketMessage(data)
  })
}

// 处理WebSocket消息
const handleWebSocketMessage = (data) => {
  const now = new Date().getHours() + ':00'
  
  switch (data.signal) {
    case 4: // 温度数据
      temperature.value = Number(data.value)
      if (tempHistory.value.length >= 24) {
        tempHistory.value.shift()
      }
      tempHistory.value.push(Number(data.value))
      break
    case 9: // 湿度数据
      humidity.value = Number(data.value)
      if (humidityHistory.value.length >= 24) {
        humidityHistory.value.shift()
      }
      humidityHistory.value.push(Number(data.value))
      break
    case 13: // 光照数据
      lightLevel.value = Number(data.value)
      break
  }
}

// 切换门锁状态
const toggleDoor = () => {
  const signal = doorLocked.value ? 6 : 7 // 6开门，7关门
  sendMQTTMessage(signal, 0)
  doorLocked.value = !doorLocked.value
}

// 切换风扇状态
const toggleFan = () => {
  const signal = fanOn.value ? 11 : 10 // 10开风扇，11关风扇
  sendMQTTMessage(signal, 0)
  fanOn.value = !fanOn.value
}

// 切换安全警报
const toggleSecurity = () => {
  securityAlert.value = !securityAlert.value
}

// 发送MQTT消息
const sendMQTTMessage = async (signal, value) => {
  try {
    const response = await uni.request({
      url: '/send',
      method: 'POST',
      data: {
        signal,
        value
      }
    })
    console.log('MQTT消息发送成功:', response)
  } catch (error) {
    console.error('MQTT消息发送失败:', error)
  }
}

// 生成时间标签（最近24小时）
const generateTimeLabels = () => {
  const labels = []
  const now = new Date()
  for (let i = 23; i >= 0; i--) {
    const time = new Date(now - i * 3600 * 1000)
    labels.push(`${time.getHours()}:00`)
  }
  return labels
}

// 初始化温度图表
const initTempChart = () => {
  const ctx = uni.createCanvasContext('tempChart')
  const chartData = {
    type: 'line',
    context: ctx,
    width: uni.upx2px(650),
    height: uni.upx2px(500),
    categories: generateTimeLabels(),
    series: [{
      name: '温度',
      data: tempHistory.value.length ? tempHistory.value : [Number(temperature.value)],
      color: '#1a73e8',
      type: 'line'
    }],
    xAxis: {
      disableGrid: true,
      scrollShow: true,
      itemCount: 8,
      labelCount: 8
    },
    yAxis: {
      gridType: 'dash',
      dashLength: 2,
      data: [{
        min: 0,
        max: 40,
        format: (val) => val + '°C'
      }]
    },
    extra: {
      line: {
        type: 'straight',
        width: 2,
        activeType: 'hollow'
      }
    },
    enableScroll: true,
    legend: { show: false },
    padding: [15, 10, 0, 15]
  }
  tempChart = new uCharts(chartData)
}

// 初始化湿度图表
const initHumidityChart = () => {
  const ctx = uni.createCanvasContext('humidityChart')
  const chartData = {
    type: 'line',
    context: ctx,
    width: uni.upx2px(650),
    height: uni.upx2px(500),
    categories: generateTimeLabels(),
    series: [{
      name: '湿度',
      data: humidityHistory.value.length ? humidityHistory.value : [Number(humidity.value)],
      color: '#1a73e8',
      type: 'line'
    }],
    xAxis: {
      disableGrid: true,
      scrollShow: true,
      itemCount: 8,
      labelCount: 8
    },
    yAxis: {
      gridType: 'dash',
      dashLength: 2,
      data: [{
        min: 0,
        max: 100,
        format: (val) => val + '%'
      }]
    },
    extra: {
      line: {
        type: 'straight',
        width: 2,
        activeType: 'hollow'
      }
    },
    enableScroll: true,
    legend: { show: false },
    padding: [15, 10, 0, 15]
  }
  humidityChart = new uCharts(chartData)
}

// 更新图表数据
const updateCharts = () => {
  if (tempChart && showTempChart.value) {
    tempChart.updateData({
      categories: generateTimeLabels(),
      series: [{
        name: '温度',
        data: tempHistory.value.length ? tempHistory.value : [Number(temperature.value)]
      }]
    })
  }
  if (humidityChart && showHumidityChart.value) {
    humidityChart.updateData({
      categories: generateTimeLabels(),
      series: [{
        name: '湿度',
        data: humidityHistory.value.length ? humidityHistory.value : [Number(humidity.value)]
      }]
    })
  }
}

// 监听图表显示状态
watch(showTempChart, (newVal) => {
  if (newVal) {
    if (!tempHistory.value.length) {
      fetchInitialData()
    }
    nextTick(() => {
      initTempChart()
    })
  }
})

watch(showHumidityChart, (newVal) => {
  if (newVal) {
    if (!humidityHistory.value.length) {
      fetchInitialData()
    }
    nextTick(() => {
      initHumidityChart()
    })
  }
})

// 监听数据变化更新图表
watch([tempHistory, humidityHistory], () => {
  updateCharts()
})

// 获取初始数据
const fetchInitialData = async () => {
  try {
    const [tempRes, humidityRes] = await Promise.all([
      uni.request({ url: '/api/ohos/temperatures' }),
      uni.request({ url: '/api/ohos/humidities' })
    ])

    if (tempRes.data && tempRes.data.length > 0) {
      // 只保留最近24小时的数据，并确保转换为数字
      tempHistory.value = tempRes.data.slice(-24).map(Number)
      temperature.value = Number(tempRes.data[tempRes.data.length - 1])
    }
    if (humidityRes.data && humidityRes.data.length > 0) {
      // 只保留最近24小时的数据，并确保转换为数字
      humidityHistory.value = humidityRes.data.slice(-24).map(Number)
      humidity.value = Number(humidityRes.data[humidityRes.data.length - 1])
    }
  } catch (error) {
    console.error('获取初始数据失败:', error)
    uni.showToast({
      title: '获取数据失败',
      icon: 'none'
    })
  }
}

// 切换底部导航
const switchTab = (tab) => {
  if (tab === currentTab.value) return
  
  switch (tab) {
    case 'monitor':
      uni.reLaunch({ url: '/pages/index/home' })
      break
    case 'ai':
      uni.reLaunch({ url: '/pages/index/ai' })
      break
    case 'person':
      uni.reLaunch({ url: '/pages/index/person' })
      break
  }
  currentTab.value = tab
}

// 在script setup部分添加跳转方法
const goToAiAnalysis = (type) => {
  const data = type === 'temperature' ? {
    type: 'temperature',
    current: temperature.value,
    history: tempHistory.value,
    unit: '°C'
  } : {
    type: 'humidity',
    current: humidity.value,
    history: humidityHistory.value,
    unit: '%'
  }
  
  uni.navigateTo({
    url: '/pages/index/ai?data=' + encodeURIComponent(JSON.stringify(data))
  })
}

onMounted(() => {
  initWebSocket()
  fetchInitialData()
})
</script>

<style scoped>
.container {
  padding: 30rpx;
  background-color: #f5f6fa;
  min-height: 100vh;
  padding-bottom: calc(100rpx + env(safe-area-inset-bottom));
}

.card {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.security-card {
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 100%);
  color: #fff;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.card-title {
  font-size: 32rpx;
  font-weight: bold;
}

.card-content {
  display: flex;
  flex-direction: column;
}

.card-content .illustration {
  object-fit: cover;
  width: 100%;
  height: 200rpx;
  margin: 20rpx 0;
}

.section {
  margin-bottom: 30rpx;
}


.control-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20rpx;
  margin-bottom: 30rpx;
}

.control-item {
  background: #fff;
  padding: 30rpx;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.control-item.active {
  background: #1a73e8;
  color: #fff;
}

.control-item.warning {
  background: #ff6b6b;
  color: #fff;
}

.control-icon {
  font-size: 48rpx;
  margin-bottom: 10rpx;
}

.control-label {
  font-size: 24rpx;
  margin-bottom: 10rpx;
}

.control-status {
  font-size: 28rpx;
  font-weight: bold;
}

.environment-section {
  display: flex;
  gap: 20rpx;
}

.env-card {
  flex: 1;
  background: #fff;
  padding: 30rpx;
  border-radius: 20rpx;
  text-align: center;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.env-title {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 10rpx;
}

.env-value {
  font-size: 36rpx;
  font-weight: bold;
  color: #1a73e8;
}

.env-tip {
  font-size: 20rpx;
  color: #999;
  margin-top: 8rpx;
}

.tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 100rpx;
  background: #ffffff;
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 100%;
}

.tab-item.active {
  color: #1a73e8;
}

.tab-icon {
  font-size: 40rpx;
  margin-bottom: 4rpx;
}

.tab-text {
  font-size: 24rpx;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  width: 90%;
  max-width: 650rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.modal-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.modal-actions {
  display: flex;
  align-items: center;
}

.modal-action-btn {
  font-size: 28rpx;
  color: #1a73e8;
  padding: 10rpx 20rpx;
  margin-right: 20rpx;
  background: rgba(26, 115, 232, 0.1);
  border-radius: 30rpx;
}

.modal-close {
  font-size: 40rpx;
  color: #999;
  padding: 10rpx;
}

.chart-container {
  width: 100%;
  height: 600rpx;
  padding: 20rpx;
}

.chart {
  width: 650rpx !important;
  height: 500rpx !important;
}
</style>
