<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">菜单管理</span>
        <el-button type="primary" size="small" style="float:right" @click="openDialog()">
          新增菜单
        </el-button>
      </div>
      <el-table :data="menus" border stripe v-loading="loading" row-key="id" default-expand-all>
        <el-table-column prop="id" label="ID" width="60"></el-table-column>
        <el-table-column prop="name" label="菜单名称" width="150"></el-table-column>
        <el-table-column prop="path" label="访问路径" width="180"></el-table-column>
        <el-table-column prop="permission" label="权限编码" width="180"></el-table-column>
        <el-table-column prop="parentId" label="上级菜单" width="80"></el-table-column>
        <el-table-column prop="sort" label="排序" width="70"></el-table-column>
        <el-table-column prop="disabled" label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.disabled === 0 ? 'success' : 'danger'" size="small">
              {{ scope.row.disabled === 0 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="small" style="color:#409EFF" @click="openDialog(scope.row)">编辑</el-button>
            <span style="color:#dcdfe6">|</span>
            <el-button
              type="text"
              size="small"
              :style="{color: scope.row.disabled === 0 ? '#E6A23C' : '#67C23A'}"
              @click="toggleMenu(scope.row.id, scope.row.disabled === 0 ? 1 : 0)"
            >
              {{ scope.row.disabled === 0 ? '禁用' : '启用' }}
            </el-button>
            <span style="color:#dcdfe6">|</span>
            <el-button type="text" size="small" style="color:#F56C6C" @click="deleteMenu(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 菜单编辑对话框 -->
    <el-dialog :title="isEdit ? '编辑菜单' : '新增菜单'" :visible.sync="dialogVisible" width="500px">
      <el-form :model="currentMenu" :rules="rules" ref="menuForm">
        <el-form-item label="菜单名称" prop="name">
          <el-input v-model="currentMenu.name" placeholder="请输入菜单名称"></el-input>
        </el-form-item>
        <el-form-item label="访问路径" prop="path">
          <el-input v-model="currentMenu.path" placeholder="如 /admin/notice"></el-input>
        </el-form-item>
        <el-form-item label="权限编码" prop="permission">
          <el-input v-model="currentMenu.permission" placeholder="如 admin:notice:manage"></el-input>
          <span style="color:#999;font-size:12px">用于后台权限判断，不影响页面显示</span>
        </el-form-item>
        <el-form-item label="上级菜单" prop="parentId">
          <el-select v-model="currentMenu.parentId" placeholder="无（顶级菜单）" clearable style="width:100%">
            <el-option :value="0" label="无（顶级菜单）"></el-option>
            <el-option v-for="m in parentMenuOptions" :key="m.id" :label="m.name" :value="m.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="currentMenu.sort" :min="0"></el-input-number>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMenu">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'MenuManage',
  data() {
    return {
      menus: [],
      loading: false,
      dialogVisible: false,
      isEdit: false,
      currentMenu: { id: null, name: '', path: '', permission: '', parentId: 0, sort: 0 },
      parentMenuOptions: [],
      rules: {
        name: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadMenus()
  },
  methods: {
    loadMenus() {
      this.loading = true
      request.get('/api/admin/menus').then(res => {
        this.menus = res.data
        this.parentMenuOptions = (res.data || []).filter(m => m.disabled === 0)
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    openDialog(row) {
      if (row) {
        this.isEdit = true
        this.currentMenu = { ...row }
        // 上级菜单不能选自己
        this.parentMenuOptions = this.menus.filter(m => m.disabled === 0 && m.id !== row.id)
      } else {
        this.isEdit = false
        this.currentMenu = { id: null, name: '', path: '', permission: '', parentId: 0, sort: 0 }
        this.parentMenuOptions = this.menus.filter(m => m.disabled === 0)
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.menuForm) this.$refs.menuForm.clearValidate()
      })
    },
    saveMenu() {
      this.$refs.menuForm.validate(valid => {
        if (!valid) return

        if (this.isEdit) {
          request.put('/api/admin/menu/update', this.currentMenu).then(() => {
            this.$message.success('更新成功')
            this.dialogVisible = false
            this.loadMenus()
          })
        } else {
          request.post('/api/admin/menu/add', this.currentMenu).then(() => {
            this.$message.success('添加成功')
            this.dialogVisible = false
            this.loadMenus()
          })
        }
      })
    },
    toggleMenu(id, disabled) {
      const action = disabled === 1 ? '禁用' : '启用'
      this.$confirm(`确认${action}该菜单吗？`, '提示', { type: 'warning' }).then(() => {
        const url = disabled === 1
          ? `/api/admin/menu/disable/${id}`
          : `/api/admin/menu/enable/${id}`
        request.put(url).then(() => {
          this.$message.success(`${action}成功`)
          this.loadMenus()
        })
      }).catch(() => {})
    },
    deleteMenu(id) {
      this.$confirm('确认删除该菜单吗？', '提示', { type: 'warning' }).then(() => {
        request.delete(`/api/admin/menu/delete/${id}`).then(() => {
          this.$message.success('删除成功')
          this.loadMenus()
        })
      }).catch(() => {})
    }
  }
}
</script>
