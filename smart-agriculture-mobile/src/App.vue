<script>
export default {
  globalData: {
    token: '',
    user: null,
    plots: [],
    currentPlotId: null
  }
}
</script>

<style>
page { background: #f8f9fa; font-family: -apple-system, BlinkMacSystemFont, sans-serif; }
.container { padding: 16px; }
.card { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.06); }
.card-title { font-size: 14px; font-weight: 600; color: #202124; margin-bottom: 12px; }
.sensor-row { display: flex; justify-content: space-between; align-items: center; padding: 10px; background: #f8f9fa; border-radius: 8px; margin: 6px 0; }
.sensor-value { font-size: 18px; font-weight: 700; }
.sensor-unit { font-size: 12px; color: #5f6368; }
.empty-state { display: flex; flex-direction: column; align-items: center; padding: 40px 20px; color: #9e9e9e; }
.tag-online { color: #34a853; }
.tag-offline { color: #e81123; }
.btn-primary { background: #1a73e8; color: #fff; border: none; border-radius: 20px; padding: 10px 24px; font-size: 14px; }
.btn-danger { background: #e81123; color: #fff; border: none; border-radius: 20px; padding: 10px 24px; font-size: 14px; }
.btn-disabled { background: #e0e0e0; color: #9e9e9e; }
</style>
