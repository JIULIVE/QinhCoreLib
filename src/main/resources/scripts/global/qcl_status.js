// QCL status script hook
// This script is intentionally lightweight so it can be edited by server owners.

function formatStatus(ctx) {
  var lines = [];
  lines.push('§6[QCL] §a脚本状态扩展已加载');
  if (ctx && ctx.player) {
    lines.push('§7player=' + ctx.player.getName());
  }
  if (ctx && ctx.vars) {
    var keys = [];
    for (var k in ctx.vars) {
      if (Object.prototype.hasOwnProperty.call(ctx.vars, k)) {
        keys.push(k);
      }
    }
    lines.push('§7variables=' + keys.length);
  }
  if (typeof qcl !== 'undefined' && qcl.bridgeStatusNames) {
    try {
      lines.push('§7bridges=' + qcl.bridgeStatusNames().join(', '));
    } catch (e) {
      lines.push('§7bridges=unavailable');
    }
  }
  lines.push('§7建议: 将复杂诊断、插件探测结果整合到 namespace 脚本中');
  return lines;
}
