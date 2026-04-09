// MessageBox 关闭动作工具方法
// 统一判断“取消/关闭”这类非确认动作，便于页面层忽略正常关闭分支。
export const isMessageBoxDismissed = (action) => action === 'cancel' || action === 'close'
