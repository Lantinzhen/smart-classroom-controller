<template>
  <view class="container">
    <!-- 数据分析模式 -->
    <template v-if="isAnalysisMode">
      <view class="header">
        <text class="title">AI 智能分析</text>
        <text class="subtitle">{{ dataType === 'temperature' ? '温度' : '湿度' }}趋势分析</text>
      </view>

      <!-- 数据概览 -->
      <view class="data-overview">
        <view class="current-value">
          <text class="label">当前{{ dataType === 'temperature' ? '温度' : '湿度' }}</text>
          <text class="value">{{ currentValue }}{{ unit }}</text>
        </view>
        <view class="stats">
          <view class="stat-item">
            <text class="stat-label">最高值</text>
            <text class="stat-value">{{ Math.max(...historyData) || 0 }}{{ unit }}</text>
          </view>
          <view class="stat-item">
            <text class="stat-label">最低值</text>
            <text class="stat-value">{{ Math.min(...historyData) || 0 }}{{ unit }}</text>
          </view>
          <view class="stat-item">
            <text class="stat-label">平均值</text>
            <text class="stat-value">{{ historyData.length ? (historyData.reduce((a, b) => a + b, 0) / historyData.length).toFixed(1) : 0 }}{{ unit }}</text>
          </view>
        </view>
      </view>

      <!-- 图表展示 -->
      <view class="chart-container">
        <canvas canvas-id="aiChart" id="aiChart" class="chart"></canvas>
      </view>

      <!-- AI分析区域 -->
      <view class="ai-analysis">
        <view class="analysis-header">
          <text class="analysis-title">AI 预测分析</text>
          <button class="refresh-btn" @click="askAI" :disabled="loading">
            <text class="refresh-icon">🔄</text>
            <text>{{ loading ? '分析中...' : '重新分析' }}</text>
          </button>
        </view>
        <view class="analysis-content" :class="{ 'loading-state': loading }">
          <text v-if="loading" class="loading">AI正在思考中...</text>
          <text v-else-if="aiResponse" class="ai-response">{{ aiResponse }}</text>
          <text v-else class="placeholder">点击"重新分析"开始AI分析</text>
        </view>
      </view>
    </template>

    <!-- 聊天模式 -->
    <template v-else>
      <view class="chat-header">
        <text class="chat-title">AI 智能助手</text>
      </view>

      <!-- 聊天内容区域 -->
      <scroll-view class="chat-container" scroll-y scroll-with-animation :scroll-top="scrollTop">
        <view class="chat-list">
          <view v-for="(msg, index) in chatMessages" :key="index" 
                :class="['chat-item', msg.type === 'user' ? 'chat-right' : 'chat-left']">
            <view class="chat-avatar">
              <text>{{ msg.type === 'user' ? '👤' : '🤖' }}</text>
            </view>
            <view class="chat-content">
              <text>{{ msg.content }}</text>
            </view>
          </view>
        </view>
      </scroll-view>

      <!-- 输入区域 -->
      <view class="chat-input-container">
        <input class="chat-input" 
               type="text" 
               v-model="inputMessage" 
               placeholder="请输入您的问题"
               :disabled="chatLoading"
               @confirm="sendMessage" />
        <button class="send-btn" 
                @click="sendMessage" 
                :disabled="!inputMessage.trim() || chatLoading">
          {{ chatLoading ? '请稍候...' : '发送' }}
        </button>
      </view>
    </template>

    <!-- 底部导航栏 -->
    <view class="tab-bar">
      <view class="tab-item" @click="switchTab('monitor')">
        <text class="tab-icon">📺</text>
        <text class="tab-text">监控</text>
      </view>
      <view class="tab-item active">
        <text class="tab-icon">🤖</text>
        <text class="tab-text">AI</text>
      </view>
      <view class="tab-item" @click="switchTab('person')">
        <text class="tab-icon">👤</text>
        <text class="tab-text">我的</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import uCharts from '@/utils/u-charts.min.js'

// 页面模式
const isAnalysisMode = ref(false)

// 分析模式数据
const dataType = ref('')
const currentValue = ref(0)
const historyData = ref([])
const unit = ref('')
const aiResponse = ref('')
const loading = ref(false)
let aiChart = null

// 聊天模式数据
const chatMessages = ref([
  { type: 'ai', content: '你好！我是AI智能助手，有什么我可以帮你的吗？' }
])
const inputMessage = ref('')
const chatLoading = ref(false)
const scrollTop = ref(0)

// 获取路由参数
onMounted(() => {
  const query = uni.getLaunchOptionsSync().query
  if (query.data) {
    isAnalysisMode.value = true
    const data = JSON.parse(decodeURIComponent(query.data))
    dataType.value = data.type
    currentValue.value = data.current
    historyData.value = data.history
    unit.value = data.unit
    initChart()
  }
})

// 初始化图表
const initChart = () => {
  const ctx = uni.createCanvasContext('aiChart')
  const labels = generateTimeLabels()
  
  const chartData = {
    type: 'line',
    context: ctx,
    width: uni.upx2px(650),
    height: uni.upx2px(500),
    categories: labels,
    series: [{
      name: dataType.value === 'temperature' ? '温度' : '湿度',
      data: historyData.value,
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
        min: Math.floor(Math.min(...historyData.value) || 0),
        max: Math.ceil(Math.max(...historyData.value) || 100),
        format: (val) => val + unit.value
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
  
  aiChart = new uCharts(chartData)
}

// 生成时间标签
const generateTimeLabels = () => {
  const labels = []
  const now = new Date()
  for (let i = historyData.value.length - 1; i >= 0; i--) {
    const time = new Date(now - i * 3600 * 1000)
    labels.push(`${time.getHours()}:00`)
  }
  return labels
}

// 请求AI分析
const askAI = async () => {
  loading.value = true
  try {
    const question = `分析这组${dataType.value === 'temperature' ? '温度' : '湿度'}数据的变化趋势，并预测接下来的变化趋势。当前值：${currentValue.value}${unit.value}，历史数据：${historyData.value.join(', ')}${unit.value}`
    
    const response = await uni.request({
      url: '/api/askDeepSeek',
      method: 'GET',
      data: {
        question
      }
    })
    
    if (response.data && response.data.answer) {
      aiResponse.value = response.data.answer
    } else {
      throw new Error('AI响应格式错误')
    }
  } catch (error) {
    console.error('AI分析请求失败:', error)
    uni.showToast({
      title: 'AI分析失败，请重试',
      icon: 'none'
    })
  } finally {
    loading.value = false
  }
}

// 发送聊天消息
const sendMessage = async () => {
  if (!inputMessage.value.trim() || chatLoading.value) return
  
  const userMessage = inputMessage.value.trim()
  chatMessages.value.push({ type: 'user', content: userMessage })
  inputMessage.value = ''
  chatLoading.value = true
  
  // 自动滚动到底部
  scrollToBottom()
  
  try {
    const response = await uni.request({
      url: '/api/askDeepSeek',
      method: 'GET',
      data: {
        question: userMessage
      }
    })
    
    if (response.data && response.data.answer) {
      chatMessages.value.push({ type: 'ai', content: response.data.answer })
    } else {
      throw new Error('AI响应格式错误')
    }
  } catch (error) {
    console.error('AI回复失败:', error)
    chatMessages.value.push({ type: 'ai', content: '抱歉，我现在无法回答，请稍后再试。' })
  } finally {
    chatLoading.value = false
    scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  setTimeout(() => {
    scrollTop.value = 9999999
  }, 100)
}

// 切换底部导航
const switchTab = (tab) => {
  switch (tab) {
    case 'monitor':
      uni.reLaunch({ url: '/pages/index/home' })
      break
    case 'person':
      uni.reLaunch({ url: '/pages/index/person' })
      break
  }
}
</script>

<style scoped>
.container {
  padding: 30rpx;
  background-color: #f5f6fa;
  min-height: 100vh;
  padding-bottom: calc(100rpx + env(safe-area-inset-bottom));
}

/* 分析模式样式 */
.header {
  margin-bottom: 30rpx;
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 100%);
  padding: 40rpx 30rpx;
  border-radius: 20rpx;
  color: #fff;
}

.title {
  font-size: 40rpx;
  font-weight: bold;
  margin-bottom: 10rpx;
  display: block;
}

.subtitle {
  font-size: 28rpx;
  opacity: 0.9;
}

.data-overview {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.current-value {
  margin-bottom: 30rpx;
  text-align: center;
}

.label {
  font-size: 28rpx;
  color: #666;
  margin-bottom: 10rpx;
  display: block;
}

.value {
  font-size: 60rpx;
  font-weight: bold;
  color: #1a73e8;
  display: block;
}

.stats {
  display: flex;
  justify-content: space-between;
  background: #f8f9fa;
  border-radius: 15rpx;
  padding: 20rpx;
}

.stat-item {
  flex: 1;
  text-align: center;
  position: relative;
}

.stat-item:not(:last-child):after {
  content: '';
  position: absolute;
  right: 0;
  top: 20%;
  height: 60%;
  width: 1rpx;
  background: #e0e0e0;
}

.stat-label {
  font-size: 24rpx;
  color: #666;
  margin-bottom: 8rpx;
  display: block;
}

.stat-value {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
  display: block;
}

.chart-container {
  background: #fff;
  border-radius: 20rpx;
  padding: 20rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.chart {
  width: 650rpx !important;
  height: 500rpx !important;
}

.ai-analysis {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.analysis-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #333;
}

.refresh-btn {
  display: flex;
  align-items: center;
  font-size: 28rpx;
  color: #fff;
  background: #1a73e8;
  padding: 10rpx 30rpx;
  border-radius: 30rpx;
  border: none;
}

.refresh-btn[disabled] {
  background: #a0c4f1;
}

.refresh-icon {
  margin-right: 10rpx;
  font-size: 32rpx;
}

.analysis-content {
  min-height: 200rpx;
  padding: 30rpx;
  background: #f8f9fa;
  border-radius: 15rpx;
  transition: all 0.3s ease;
}

.analysis-content.loading-state {
  background: #f0f7ff;
}

.loading {
  color: #1a73e8;
  font-size: 28rpx;
  display: block;
  text-align: center;
}

.ai-response {
  color: #333;
  font-size: 28rpx;
  line-height: 1.6;
}

.placeholder {
  color: #999;
  font-size: 28rpx;
  text-align: center;
  display: block;
}

/* 聊天模式样式 */
.chat-header {
  background: linear-gradient(135deg, #1a73e8 0%, #0d47a1 100%);
  padding: 40rpx 30rpx;
  border-radius: 20rpx;
  color: #fff;
  margin-bottom: 30rpx;
}

.chat-title {
  font-size: 40rpx;
  font-weight: bold;
}

.chat-container {
  width: fit-content;
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 120rpx;
  box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.05);
  height: calc(100vh - 400rpx);
}

.chat-list {
  padding-bottom: 30rpx;
}

.chat-item {
  display: flex;
  margin-bottom: 30rpx;
  align-items: flex-start;
}

.chat-left {
  flex-direction: row;
}

.chat-right {
  flex-direction: row-reverse;
}

.chat-avatar {
  width: 80rpx;
  height: 80rpx;
  background: #f0f7ff;
  border-radius: 40rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  flex-shrink: 0;
}

.chat-content {
  max-width: 70%;
  margin: 0 20rpx;
  padding: 20rpx;
  border-radius: 10rpx;
  font-size: 28rpx;
  line-height: 1.5;
}

.chat-left .chat-content {
  background: #f0f7ff;
  color: #333;
}

.chat-right .chat-content {
  background: #1a73e8;
  color: #fff;
}

.chat-input-container {
  position: fixed;
  bottom: calc(100rpx + env(safe-area-inset-bottom));
  left: 0;
  right: 0;
  padding: 20rpx 30rpx;
  background: #fff;
  display: flex;
  align-items: center;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
}

.chat-input {
  flex: 1;
  height: 80rpx;
  background: #f5f6fa;
  border-radius: 40rpx;
  padding: 0 30rpx;
  font-size: 28rpx;
  margin-right: 20rpx;
}

.send-btn {
  width: 160rpx;
  height: 80rpx;
  background: #1a73e8;
  color: #fff;
  border-radius: 40rpx;
  font-size: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

.send-btn[disabled] {
  background: #a0c4f1;
}

/* 底部导航栏样式 */
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
</style>
