const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, LevelFormat, HeadingLevel, BorderStyle, WidthType, ShadingType,
        VerticalAlign } = require('docx');
const fs = require('fs');

// ============ 样式常量 ============
const HEI = '黑体';   // 标题字体
const SONG = '宋体';  // 正文字体
const border = { style: BorderStyle.SINGLE, size: 1, color: '999999' };
const borders = { top: border, bottom: border, left: border, right: border };
const CONTENT_W = 9026; // A4 1英寸边距内容宽

function h1(text) {
  return new Paragraph({ heading: HeadingLevel.HEADING_1, spacing: { before: 300, after: 200 },
    children: [new TextRun({ text, font: HEI, size: 32, bold: true, color: '1F3864' })] });
}
function h2(text) {
  return new Paragraph({ heading: HeadingLevel.HEADING_2, spacing: { before: 200, after: 120 },
    children: [new TextRun({ text, font: HEI, size: 26, bold: true, color: '2E5395' })] });
}
function body(text, opts = {}) {
  return new Paragraph({ spacing: { line: 360, after: 60 }, indent: { firstLine: 480 },
    children: [new TextRun({ text, font: SONG, size: 22, ...opts })] });
}
function bullet(text, bold = false) {
  return new Paragraph({ numbering: { reference: 'bullets', level: 0 }, spacing: { line: 340, after: 40 },
    children: [new TextRun({ text, font: SONG, size: 22, bold })] });
}
function caption(text) {
  return new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 60, after: 160 },
    children: [new TextRun({ text, font: SONG, size: 18, color: '808080', italics: true })] });
}
function cell(text, width, opts = {}) {
  return new TableCell({
    borders, width: { size: width, type: WidthType.DXA },
    verticalAlign: VerticalAlign.CENTER,
    shading: opts.fill ? { fill: opts.fill, type: ShadingType.CLEAR } : undefined,
    margins: { top: 60, bottom: 60, left: 100, right: 100 },
    children: [new Paragraph({ alignment: opts.center !== false ? AlignmentType.CENTER : undefined,
      children: [new TextRun({ text, font: SONG, size: opts.bold ? 21 : 20, bold: opts.bold })] })]
  });
}

const doc = new Document({
  numbering: { config: [{
    reference: 'bullets',
    levels: [{ level: 0, format: LevelFormat.BULLET, text: '●', alignment: AlignmentType.LEFT,
      style: { paragraph: { indent: { left: 520, hanging: 260 } } } }]
  }]},
  styles: {
    default: { document: { run: { font: SONG, size: 22 } } },
    paragraphStyles: [
      { id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 32, bold: true, font: HEI, color: '1F3864' },
        paragraph: { spacing: { before: 300, after: 200 }, outlineLevel: 0 } },
      { id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
        run: { size: 26, bold: true, font: HEI, color: '2E5395' },
        paragraph: { spacing: { before: 200, after: 120 }, outlineLevel: 1 } },
    ]
  },
  sections: [{
    properties: { page: {
      size: { width: 11906, height: 16838 }, // A4
      margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
    }},
    children: [
      // ==================== 封面 ====================
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 2400, after: 400 },
        children: [new TextRun({ text: '第二十一届宋庆龄少年儿童发明奖', font: HEI, size: 36, bold: true, color: '1F3864' })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 200 },
        children: [new TextRun({ text: '发明作品说明文档', font: HEI, size: 28, color: '2E5395' })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 1600 },
        children: [new TextRun({ text: '（中学组）', font: SONG, size: 24, color: '404040' })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 800, after: 200 },
        children: [new TextRun({ text: '室内火灾智能动态疏散灯牌系统', font: HEI, size: 44, bold: true, color: '000000' })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 1400, after: 120 },
        children: [new TextRun({ text: '作者：朱宇轩  陈麒安  姚润泽', font: SONG, size: 24 })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
        children: [new TextRun({ text: '指导教师：于双源', font: SONG, size: 24 })] }),
      new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 120 },
        children: [new TextRun({ text: '安吉蓝润天使外国语实验学校', font: SONG, size: 24 })] }),
      new Paragraph({ alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: '2026 年 5 月', font: SONG, size: 24 })] }),

      // ==================== 一、作品概述 ====================
      h1('一、作品概述'),
      body('火灾发生时，每一秒都关乎生命。被困人员在浓烟中慌乱寻找出口，而传统疏散指示牌方向固定，火势一旦变化就可能把人引向死路；消防员冲进火场后，也不清楚内部的人员分布和火势走向，救援如同"盲人摸象"。'),
      body('针对上述痛点，我们设计并制作了"室内火灾智能动态疏散灯牌系统"。系统由感知层、引导层、通信层、部署层四层协同组成：每个灯牌点位集成多类型传感器实时监测环境；ESP32 主控运行 A* 路径规划算法，将着火点动态标记为障碍物，计算每个灯牌到最近安全出口的最优路径并点亮对应方向的箭头；火场态势通过无线通信同步至消防员终端 APP，实现疏散引导与消防救援的双向协同。'),
      body('本作品以 10×5 沙盘模型完成系统验证：13 个动态方向灯牌、4 个安全出口、13 路火焰传感器，配套消防员终端 APP 与语音疏散播报模块。沙盘测试中，单点起火场景疏散路径缩短约 30%，多点起火场景灯牌全部正确绕开危险区域，验证了技术方案的可行性。'),

      // ==================== 二、问题提出 ====================
      h1('二、问题提出'),
      h2('2.1 传统疏散指示的局限'),
      body('建筑火灾造成的人员伤亡中，疏散延误是关键因素。经文献调研与案例分析，我们发现传统疏散系统存在两大痛点：一是固定路标无法适应火势的动态变化，恐慌中的被困人员极易被引入危险区域；二是消防员进入火场后难以获知内部人员分布与火势变化，无法精准救援。'),
      h2('2.2 人员密集场所的迫切需求'),
      body('商场、医院、学校等人员密集场所疏散路径复杂，老人与儿童更难在浓烟中快速找到出口。火灾中每延误一秒，伤亡风险就成倍增加。亟需一种能实时感知火情、动态规划逃生路径并辅助消防指挥的智能疏散系统。'),
      h2('2.3 现有方案的不足'),
      bullet('现有智能疏散方案多采用 Zigbee 或 RFID 技术，硬件成本高，老旧建筑难以改造；'),
      bullet('多数方案仅做单向引导，只让人员往外跑，消防员无法获知火场内部态势；'),
      bullet('部分系统专用性强，仅限地铁、隧道等场景，难以推广到住宅、学校等日常场所。'),

      // ==================== 三、系统总体设计 ====================
      h1('三、系统总体设计'),
      h2('3.1 四层协同架构'),
      body('系统由感知层、引导层、通信层、部署层四层协同组成，实现"感知—决策—引导—通信"的完整闭环：'),
      bullet('感知层：在每个灯牌点位部署烟雾、温度、火焰等传感器，实时采集环境数据，判断火灾是否发生及火势强度与蔓延方向；', true),
      bullet('引导层：灯牌采用 LED 箭头阵列，由 ESP32 主控驱动。系统以室内平面图为网格地图，运行 A* 路径规划算法动态计算逃生方向；', true),
      bullet('通信层：基于 ESP32 无线通信模块，将险情位置、火势强度等信息同步传输至消防员终端 APP；', true),
      bullet('部署层：将灯牌布设于走廊、楼梯口、转角等关键位置，形成连续指引链路。', true),
      h2('3.2 沙盘模型设计'),
      body('系统在 10 列 × 5 行的沙盘地图上进行验证。地图包含墙壁、通道、房间与 4 个安全出口，布设 13 个双色 LED 灯牌，每个灯牌旁配置火焰传感器，模拟真实建筑中关键点位的部署。'),
      new Table({
        width: { size: CONTENT_W, type: WidthType.DXA },
        columnWidths: [2000, 3513, 3513],
        rows: [
          new TableRow({ children: [cell('地图规格', 2000, { bold: true, fill: 'D5E8F0' }), cell('10 列 × 5 行网格', 3513, { fill: 'D5E8F0' }), cell('模拟室内平面图', 3513, { fill: 'D5E8F0' })] }),
          new TableRow({ children: [cell('灯牌数量', 2000, { bold: true }), cell('13 个双色 LED 灯牌', 3513), cell('每个含 2 颗绿/红/黄三色 LED', 3513)] }),
          new TableRow({ children: [cell('安全出口', 2000, { bold: true }), cell('4 个', 3513), cell('顶部、左侧、右侧、底部各 1 个', 3513)] }),
          new TableRow({ children: [cell('火焰传感器', 2000, { bold: true }), cell('13 路', 3513), cell('每个灯牌对应 1 路，实时监测', 3513)] }),
          new TableRow({ children: [cell('LED 驱动', 2000, { bold: true }), cell('74HC595 × 9 级联', 3513), cell('移位寄存器串行驱动 72 位', 3513)] }),
          new TableRow({ children: [cell('语音模块', 2000, { bold: true }), cell('双通道触发', 3513), cell('疏散播报 + 救援播报', 3513)] }),
        ]
      }),

      // ==================== 四、硬件系统设计 ====================
      h1('四、硬件系统设计'),
      h2('4.1 核心硬件清单'),
      new Table({
        width: { size: CONTENT_W, type: WidthType.DXA },
        columnWidths: [2000, 2513, 4513],
        rows: [
          new TableRow({ children: [cell('组件', 2000, { bold: true, fill: 'E8E8E8' }), cell('型号/规格', 2513, { bold: true, fill: 'E8E8E8' }), cell('功能说明', 4513, { bold: true, fill: 'E8E8E8' })] }),
          new TableRow({ children: [cell('主控板', 2000), cell('ESP32-WROOM-32', 2513), cell('运行A*算法、无线通信、控制LED阵列', 4513)] }),
          new TableRow({ children: [cell('火焰传感器', 2000), cell('KY-026 ×13', 2513), cell('检测760-1100nm红外火焰辐射', 4513)] }),
          new TableRow({ children: [cell('烟雾传感器', 2000), cell('MQ-2', 2513), cell('检测烟雾浓度，模拟量输出', 4513)] }),
          new TableRow({ children: [cell('温度传感器', 2000), cell('DS18B20', 2513), cell('检测环境温度，精度±0.5℃', 4513)] }),
          new TableRow({ children: [cell('CO传感器', 2000), cell('MQ-7', 2513), cell('检测一氧化碳浓度', 4513)] }),
          new TableRow({ children: [cell('LED灯牌', 2000), cell('双色LED ×26', 2513), cell('显示方向箭头，绿/红/黄三色', 4513)] }),
          new TableRow({ children: [cell('LED驱动', 2000), cell('74HC595 ×9', 2513), cell('移位寄存器级联驱动', 4513)] }),
          new TableRow({ children: [cell('语音模块', 2000), cell('双通道模块', 2513), cell('疏散/救援语音循环播报', 4513)] }),
        ]
      }),
      caption('表 1  核心硬件清单'),
      h2('4.2 灯牌设计'),
      body('每个灯牌包含 2 颗双色 LED，按灯牌朝向分为横向（左右）与纵向（上下）两类。灯牌支持四种工作状态：'),
      bullet('方向指引：单颗绿色 LED 点亮，指示左/右或上/下；'),
      bullet('到达出口：双绿 LED 同时点亮，表示此处即为安全出口；'),
      bullet('被困警告：双黄 LED 闪烁，表示该区域已无安全逃生路径，提示人员就近避险等待救援；'),
      bullet('熄灭：灯牌所在位置已着火，自动熄灭。'),
      body('火灾发生时，方向灯牌以 500ms 周期双闪，增强视觉冲击力，确保浓烟环境中依然醒目。'),

      // ==================== 五、核心算法设计 ====================
      h1('五、核心算法设计：A* 动态路径规划'),
      h2('5.1 算法原理'),
      body('系统以室内平面图为网格地图，将墙壁标记为永久障碍物，着火点动态标记为临时障碍物。以每个灯牌位置为起点、最近安全出口为终点，运行 A* 算法搜索最短路径。A* 算法结合了广度优先搜索的完备性与贪心搜索的效率：'),
      bullet('代价函数 f(n) = g(n) + h(n)：g(n) 为起点到当前格的实际代价，h(n) 为当前格到终点的启发式估计；'),
      bullet('启发式采用曼哈顿距离 |dx| + |dy|，保证在网格地图上找到最优解；'),
      bullet('开放集采用线性扫描选取最小 f 值节点——地图仅 50 格，线性扫描比堆维护更高效，也降低了 ESP32 内存开销。'),
      h2('5.2 动态避障策略'),
      bullet('灯牌自身传感器触发 → 该灯牌熄灭，防止误导疏散人员；'),
      bullet('灯牌最近可走格位于火点 → BFS 搜索附近安全格作为新起点重新寻路；'),
      bullet('灯牌周围全部着火 → 无可达出口 → 切换黄闪警告模式（被困区域标识）；'),
      bullet('灯牌位于出口 → 直接指向出口方向，无需寻路。'),
      h2('5.3 系统工作流程'),
      body('(1) 读取各灯牌点位的传感器数据（火焰、烟雾、温度等）；(2) 判断是否检测到火情：任一传感器超过预设阈值即判定为异常；(3) 将异常区域对应的网格标记为障碍物；(4) 以每个灯牌为起点运行 A* 算法搜索到最近安全出口的最短路径；(5) 根据计算结果点亮各灯牌对应逃生方向的箭头 LED；(6) 循环执行步骤 1-5，随火情变化实时刷新路径与灯牌方向。'),

      // ==================== 六、消防员终端 APP ====================
      h1('六、消防员终端 APP 设计'),
      h2('6.1 设计目标'),
      body('消防员在进入火场前，通过手机 APP 无线连接 ESP32 主控，实时掌握火场态势：哪里有火、哪些区域有人员被困、哪些出口仍然安全、火势朝哪个方向蔓延——相当于拥有了"火场内的眼睛"。'),
      h2('6.2 三页面架构'),
      bullet('设备连接页：扫描并连接 FIRE_CTRL 设备，显示连接状态与信号强度；'),
      bullet('火场地图页（核心）：以深色主题 Canvas 实时渲染 10×5 沙盘地图，自动旋转适配竖屏，支持双指缩放、单指平移；'),
      bullet('分析面板页：出口状态、语音播报、火势趋势、火灾时间线。'),
      h2('6.3 火场地图渲染管线'),
      body('地图采用 Canvas 五层渲染管线，各层独立绘制、按序叠加：'),
      bullet('网格层：墙壁（砖纹）、通道、出口（绿色辉光边框）、网格线；'),
      bullet('优先级层：按救援优先级对灯牌区域染色——P0 被困区红色脉冲边框、P1 高优先级橙色、P2 注意监控黄色，并通过 BFS 沿通道向四周扩散，直观展示需要救援的范围；'),
      bullet('预警层：被困区域黄色波纹扩散动画；'),
      bullet('火点层：火灾点红色径向辉光，呼吸脉冲动画；'),
      bullet('灯牌层：方向箭头（统一尺寸三角图形，质心居中），火情下 500ms 周期闪烁。'),
      h2('6.4 救援优先级算法'),
      body('APP 自动为每个灯牌区域计算救援优先级，帮助消防员确定"哪里必须第一时间救援"：'),
      bullet('P0 立即救援：灯牌显示黄闪（被困区域），得分 ≥ 100；'),
      bullet('P1 高优先级：距离火点 2 格以内，得分 ≥ 50；'),
      bullet('P2 注意监控：距离火点 3-5 格，得分 ≥ 20；'),
      bullet('P3 安全区域：其余区域。'),
      h2('6.5 无线通信协议'),
      body('APP 与 ESP32 之间采用 JSON 格式的无线消息协议。连接建立后，ESP32 推送地图静态配置（网格、墙壁、出口、灯牌位置），此后实时推送动态状态：火灾点列表、灯牌方向、传感器状态、语音模式，并以心跳包维持连接健康。APP 端支持双向命令：手动设置灯牌方向、模拟增删火点、系统复位等，便于消防演练与功能演示。'),
      new Table({
        width: { size: CONTENT_W, type: WidthType.DXA },
        columnWidths: [2813, 3100, 3113],
        rows: [
          new TableRow({ children: [cell('消息类型', 2813, { bold: true, fill: 'E8E8E8' }), cell('方向', 3100, { bold: true, fill: 'E8E8E8' }), cell('内容', 3113, { bold: true, fill: 'E8E8E8' })] }),
          new TableRow({ children: [cell('MAP_CONFIG', 2813), cell('设备 → APP', 3100), cell('10×5网格、墙壁、出口', 3113)] }),
          new TableRow({ children: [cell('LIGHT_CONFIG', 2813), cell('设备 → APP', 3100), cell('13个灯牌坐标与类型', 3113)] }),
          new TableRow({ children: [cell('FIRE_UPDATE', 2813), cell('设备 → APP', 3100), cell('火灾点坐标列表', 3113)] }),
          new TableRow({ children: [cell('DIRECTION_UPDATE', 2813), cell('设备 → APP', 3100), cell('各灯牌方向值', 3113)] }),
          new TableRow({ children: [cell('VOICE_MODE', 2813), cell('设备 → APP', 3100), cell('语音模式（怠速/疏散/救援）', 3113)] }),
          new TableRow({ children: [cell('SENSOR_STATE', 2813), cell('设备 → APP', 3100), cell('13路传感器状态', 3113)] }),
          new TableRow({ children: [cell('HEARTBEAT', 2813), cell('设备 → APP', 3100), cell('系统心跳', 3113)] }),
          new TableRow({ children: [cell('控制命令', 2813), cell('APP → 设备', 3100), cell('设灯牌/加火点/复位', 3113)] }),
        ]
      }),
      caption('表 2  无线通信协议'),

      // ==================== 七、语音疏散播报 ====================
      h1('七、语音疏散播报系统'),
      body('系统集成双通道语音模块，根据火情状态自动循环播报：'),
      bullet('安全模式：无火灾，不播报；'),
      bullet('疏散模式（有火情、无被困区域）：每 10 秒播报"室内起火，立刻按应急绿灯指示方向逃离"；'),
      bullet('救援模式（存在被困区域）：以 20 秒为周期交替播报"双闪区域立刻进入有窗房间，湿衣物堵住门缝口鼻，等待救援"与疏散指引。'),
      body('语音播报状态同步显示在消防员 APP 的分析面板中，消防员可随时确认现场正在播放的引导内容。'),

      // ==================== 八、测试与效果 ====================
      h1('八、沙盘测试与效果验证'),
      h2('8.1 测试方法'),
      body('在沙盘模型中设置多条走廊、多个房间及四个安全出口，在走廊中部和房间内分别模拟起火，记录系统的响应时间与路径调整情况。'),
      h2('8.2 测试结果'),
      new Table({
        width: { size: CONTENT_W, type: WidthType.DXA },
        columnWidths: [2233, 2369, 1688, 2736],
        rows: [
          new TableRow({ children: [cell('测试场景', 2233, { bold: true, fill: 'E8E8E8' }), cell('起火位置', 2369, { bold: true, fill: 'E8E8E8' }), cell('响应时间', 1688, { bold: true, fill: 'E8E8E8' }), cell('路径调整结果', 2736, { bold: true, fill: 'E8E8E8' })] }),
          new TableRow({ children: [cell('单点起火', 2233), cell('走廊中部', 2369), cell('1-2秒', 1688), cell('箭头从指向A出口切换为B出口', 2736)] }),
          new TableRow({ children: [cell('单点起火', 2233), cell('房间内部', 2369), cell('1-2秒', 1688), cell('箭头避开该房间方向，指向远端出口', 2736)] }),
          new TableRow({ children: [cell('多点起火', 2233), cell('走廊+房间', 2369), cell('2-3秒', 1688), cell('重新规划绕行路径，避开两个火点', 2736)] }),
          new TableRow({ children: [cell('火势蔓延', 2233), cell('走廊逐步扩散', 2369), cell('每3-5秒更新', 1688), cell('箭头随火势动态调整方向', 2736)] }),
        ]
      }),
      caption('表 3  沙盘测试记录'),
      h2('8.3 效果分析'),
      bullet('路径规划效果：对比传统固定指示牌，单点起火场景下疏散路径缩短约 30%；多点起火情况下所有灯牌均正确绕开危险区域；'),
      bullet('消防员终端效果：通过 APP 可实时查看传感器状态、灯牌工作状态、火灾时间线与火势趋势，相当于拥有"火场内的眼睛"；'),
      bullet('系统稳定性：多次沙盘演示中系统响应稳定，灯牌切换流畅，无线通信正常。'),

      // ==================== 九、创新点 ====================
      h1('九、创新点分析'),
      h2('9.1 A* 动态路径规划'),
      body('将室内平面图网格化，根据多传感器数据动态标记障碍区域，周期性重算最优逃生路径并更新灯牌指向，突破传统固定指向的局限。灯牌从"固定指示"升级为"动态导航"。'),
      h2('9.2 疏散救援双向协同'),
      body('现有方案多为单向引导，本系统同步将火场态势——起火位置、被困区域、出口状态、火势趋势——实时传输至消防员终端，辅助精准救援，实现疏散与救援联动。救援优先级算法自动标注必须第一时间救援的区域。'),
      h2('9.3 日常应急双模式集成'),
      body('灯牌常态下作为室内导向标识，火灾时自动切换疏散模式，一套硬件兼顾双重功能，无需额外维护成本。基于 ESP32 开源平台，硬件成本低，老旧建筑也可加装改造。'),

      // ==================== 十、开发历程 ====================
      h1('十、开发历程'),
      new Table({
        width: { size: CONTENT_W, type: WidthType.DXA },
        columnWidths: [1713, 4200, 3113],
        rows: [
          new TableRow({ children: [cell('阶段', 1713, { bold: true, fill: 'E8E8E8' }), cell('工作内容', 4200, { bold: true, fill: 'E8E8E8' }), cell('成果', 3113, { bold: true, fill: 'E8E8E8' })] }),
          new TableRow({ children: [cell('10-11月', 1713), cell('调研现有方案，学习ESP32开发基础', 4200), cell('确定技术路线', 3113)] }),
          new TableRow({ children: [cell('11-12月', 1713), cell('传感器选型与调试，搭建单节点原型', 4200), cell('单灯牌原型可采集数据', 3113)] }),
          new TableRow({ children: [cell('12-1月', 1713), cell('实现A*算法并移植到ESP32', 4200), cell('算法在模拟地图上运行通过', 3113)] }),
          new TableRow({ children: [cell('1-3月', 1713), cell('制作沙盘模型，多节点联调', 4200), cell('沙盘演示系统跑通', 3113)] }),
          new TableRow({ children: [cell('3-4月', 1713), cell('开发APP与后台，集成测试', 4200), cell('数据上传与远程监控可用', 3113)] }),
        ]
      }),
      caption('表 4  开发历程'),

      // ==================== 十一、优点与展望 ====================
      h1('十一、优点与进一步研究'),
      h2('11.1 系统优点'),
      bullet('实时自适应：灯牌方向随火情变化动态调整，避免固定路标指向死路；'),
      bullet('双向价值：既引导群众疏散，又为消防员提供火场态势，提升救援效率；'),
      bullet('低成本易推广：基于 ESP32 开源平台，硬件成本低，可融入既有建筑消防系统；'),
      bullet('通用性强：适用于商场、学校、医院等各类室内场所，箭头指引直观，对老人儿童同样友好。'),
      h2('11.2 还需进一步研究的问题'),
      bullet('传感器融合与误报：优化多源数据融合算法，减少烟雾、温度等传感器的误报漏报；'),
      bullet('设备与通信可靠性：火场可能破坏灯牌设备或通信信号，需研究备用通信方案（如 LoRa 或自组网）；'),
      bullet('人员密度感知：目前无直接人员定位，可研究结合红外或毫米波雷达估算各区域人数，优化疏散策略；'),
      bullet('电源冗余：火灾可能导致断电，需研究备用电池及低功耗设计。'),
      body('后续将围绕上述方向持续优化，推动系统实用化。'),

      // ==================== 开发心得 ====================
      h1('十二、开发心得'),
      body('我们小组在做这个项目时，遇到了一个很难解决的问题：把 A* 算法放到 ESP32 上运行。一开始直接用原来的代码，总是出现内存不够、系统卡顿的情况。后来我们一起讨论、反复试验，把网格画得更简单、缩小搜索范围、用线性扫描代替复杂数据结构，一点点调试，终于把问题解决了，现在系统 1-2 秒就能快速算出新路线。'),
      body('通过这次合作研发，我们发现课本上的知识和动手做项目很不一样，需要不断尝试、互相配合才能成功。接下来我们还想继续优化，让传感器更准确、更少误报，希望这个系统能真正帮到大家！'),
    ]
  }]
});

Packer.toBuffer(doc).then(buffer => {
  fs.writeFileSync('D:/ProgramData/FirefighterApp/docs/项目说明书-室内火灾智能动态疏散灯牌系统.docx', buffer);
  console.log('DONE: 项目说明书 generated');
});
