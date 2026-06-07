// QinhCoreLib 示例脚本 — 复制到 plugins/QinhCoreLib/scripts/global/example.js
// GUI 条件: type=javascript value=global:example.js:canOpen
// GUI 动作: type=javascript value=global:example.js:onClick

function main(ctx) {
    qcl.logInfo("example.js main() 被调用，玩家=" + (ctx.player() ? ctx.player().getName() : "null"));
    return true;
}

function canOpen(ctx) {
    var player = ctx.player();
    if (!player) {
        return false;
    }
    return player.getLevel() >= 0;
}

function onClick(ctx) {
    var player = ctx.player();
    if (!player) {
        return false;
    }
    qcl.logInfo("onClick: " + player.getName());
    return true;
}
