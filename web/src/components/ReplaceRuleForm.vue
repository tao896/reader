<template>
  <el-dialog
    title="替换规则"
    :visible.sync="show"
    :width="dialogWidth"
    :top="dialogTop"
    :fullscreen="$store.state.miniInterface"
    :class="
      isWebApp && !$store.getters.isNight ? 'status-bar-light-bg-dialog' : ''
    "
    v-if="$store.getters.isNormalPage"
    :before-close="cancel"
  >
    <el-form :model="ruleForm" label-width="80px">
      <el-form-item label="名称">
        <el-input v-model="ruleForm.name"></el-input>
      </el-form-item>
      <el-form-item label="类型">
        <el-radio-group v-model="ruleForm.isRegex">
          <el-radio-button :label="false">关键词</el-radio-button>
          <el-radio-button :label="true">正则</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="规则">
        <el-input v-model="ruleForm.pattern"></el-input>
      </el-form-item>
      <el-form-item label="替换为">
        <el-input
          v-model="ruleForm.replacement"
          placeholder="留空表示删除匹配文本"
        ></el-input>
      </el-form-item>
      <el-form-item label="替换范围">
        <el-input
          v-model="ruleForm.scope"
          placeholder="留空表示全局过滤"
        ></el-input>
      </el-form-item>
      <el-form-item label="是否启用">
        <el-switch
          v-model="ruleForm.isEnabled"
          active-color="#13ce66"
          inactive-color="#ff4949"
          :active-value="true"
          :inactive-value="false"
        >
        </el-switch>
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button size="medium" @click="cancel">取 消</el-button>
      <el-button size="medium" type="primary" @click="save">确 定</el-button>
    </div>
  </el-dialog>
</template>

<script>
import { mapGetters } from "vuex";
import Axios from "../plugins/axios";
import { defaultReplaceRule } from "../plugins/config.js";

export default {
  model: {
    prop: "show",
    event: "setShow"
  },
  name: "ReplaceRuleForm",
  data() {
    return {
      ruleForm: { ...defaultReplaceRule }
    };
  },
  props: ["show", "rule", "isAdd"],
  computed: {
    ...mapGetters(["dialogWidth", "dialogTop", "dialogContentHeight"])
  },
  watch: {
    show(isVisible) {
      if (isVisible) {
        this.ruleForm = this.normalizeRuleForm(this.rule);
      }
    }
  },
  methods: {
    normalizeRuleForm(rule) {
      const ruleForm = { ...defaultReplaceRule, ...(rule || {}) };
      return {
        ...ruleForm,
        replacement: ruleForm.replacement || "",
        scope: ruleForm.scope || "",
        isRegex: ruleForm.isRegex === true,
        isEnabled: ruleForm.isEnabled !== false
      };
    },
    cancel() {
      this.$emit("setShow", false);
    },
    save() {
      const rule = this.normalizeRuleForm(this.ruleForm);
      rule.name = rule.name.replace(/^\s+/, "").replace(/\s+$/, "");
      rule.scope = rule.scope.replace(/^\s+/, "").replace(/\s+$/, "");
      if (!rule.name) {
        this.$message.error("规则名不能为空");
        return;
      }
      if (!rule.pattern) {
        this.$message.error("规则不能为空");
        return;
      }
      if (rule.isRegex) {
        try {
          new RegExp(rule.pattern, "ig");
        } catch (error) {
          this.$message.error("正则表达式格式错误");
          return;
        }
      }
      if (this.isAdd) {
        // 判断 name 是否唯一
        const isExisted = this.$store.state.filterRules.find(
          v => v.name === rule.name
        );
        if (isExisted) {
          this.$message.error("规则名不能重复");
          return;
        }
      }
      // this.$store.commit("addFilterRule", rule);
      Axios.post("/saveReplaceRule", rule).then(
        res => {
          if (res.data.isSuccess) {
            this.$message.success(
              (this.isAdd ? "新增" : "编辑") + "替换规则成功"
            );
            this.$root.$children[0].loadReplaceRules(true);
            this.cancel();
          }
        },
        error => {
          this.$message.error(
            (this.isAdd ? "新增" : "编辑") +
              "替换规则失败 " +
              (error && error.toString())
          );
        }
      );
    }
  }
};
</script>
<style lang="stylus" scoped>
.float-left {
  float: left;
}
</style>
