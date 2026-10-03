<template>
  <div>
    <el-card>
      <div slot="header">
        <span style="font-size:16px;font-weight:bold">角色管理</span>
        <el-button type="primary" size="small" style="float:right" @click="openDialog()">新增角色</el-button>
      </div>
      <el-table :data="roles" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="60"></el-table-column>
        <el-table-column prop="name" label="角色名称" width="150"></el-table-column>
        <el-table-column prop="code" label="角色标识"></el-table-column>
        <el-table-column label="操作" width="250" align="center">
          <template slot-scope="scope">
            <el-button type="text" size="small" style="color:#409EFF" @click="assignMenus(scope.row)">分配菜单</el-button>
            <span style="color:#dcdfe6">|</span>
            <el-button type="text" size="small" style="color:#409EFF" @click="openDialog(scope.row)">编辑</el-button>
            <span style="color:#dcdfe6">|</span>
            <el-button type="text" size="small" style="color:#F56C6C" @click="deleteRole(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 角色编辑对话框 -->
    <el-dialog :title="isEdit ? '编辑角色' : '新增角色'" :visible.sync="dialogVisible" width="450px">
      <el-form :model="currentRole" :rules="rules" ref="roleForm">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="currentRole.name" placeholder="请输入角色名称"></el-input>
        </el-form-item>
        <el-form-item label="角色标识" prop="code">
          <el-input v-model="currentRole.code" placeholder="如 ROLE_ADMIN"></el-input>
          <span style="color:#999;font-size:12px">系统内部使用，如 ROLE_ADMIN、ROLE_TEACHER</span>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRole">保存</el-button>
      </span>
    </el-dialog>

    <!-- 分配菜单对话框 -->
    <el-dialog title="分配菜单" :visible.sync="menuVisible" width="450px">
      <el-tree
        ref="menuTree"
        :data="menuTree"
        show-checkbox
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        :default-checked-keys="checkedMenuIds"
      ></el-tree>
      <span slot="footer">
        <el-button @click="menuVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMenus">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'RoleManage',
  data() {
    return {
      roles: [],
      loading: false,
      dialogVisible: false,
      isEdit: false,
      currentRole: { id: null, name: '', code: '' },
      rules: {
        name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
        code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
      },
      menuVisible: false,
      menuTree: [],
      checkedMenuIds: [],
      currentRoleId: null
    }
  },
  created() {
    this.loadRoles()
  },
  methods: {
    loadRoles() {
      this.loading = true
      request.get('/api/admin/roles').then(res => {
        this.roles = res.data
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    openDialog(row) {
      if (row) {
        this.isEdit = true
        this.currentRole = { ...row }
      } else {
        this.isEdit = false
        this.currentRole = { id: null, name: '', code: '' }
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.roleForm) this.$refs.roleForm.clearValidate()
      })
    },
    saveRole() {
      this.$refs.roleForm.validate(valid => {
        if (!valid) return
        if (this.isEdit) {
          request.put('/api/admin/role/update', this.currentRole).then(() => {
            this.$message.success('更新成功')
            this.dialogVisible = false
            this.loadRoles()
          })
        } else {
          request.post('/api/admin/role/add', this.currentRole).then(() => {
            this.$message.success('添加成功')
            this.dialogVisible = false
            this.loadRoles()
          })
        }
      })
    },
    deleteRole(id) {
      this.$confirm('确认删除该角色吗？', '提示', { type: 'warning' }).then(() => {
        request.delete(`/api/admin/role/delete/${id}`).then(() => {
          this.$message.success('删除成功')
          this.loadRoles()
        })
      }).catch(() => {})
    },
    assignMenus(row) {
      this.currentRoleId = row.id
      // 获取所有菜单树
      request.get('/api/admin/role/all-menus').then(res => {
        this.menuTree = this.buildTree(res.data)
        // 获取当前角色已有菜单
        request.get(`/api/admin/role/menus/${row.id}`).then(r => {
          this.checkedMenuIds = r.data
          this.menuVisible = true
        })
      })
    },
    buildTree(list) {
      const map = {}
      const tree = []
      list.forEach(item => { map[item.id] = { ...item, children: [] } })
      list.forEach(item => {
        if (item.parentId && item.parentId !== 0 && map[item.parentId]) {
          map[item.parentId].children.push(map[item.id])
        } else if (!item.parentId || item.parentId === 0) {
          tree.push(map[item.id])
        }
      })
      return tree
    },
    saveMenus() {
      const checkedKeys = this.$refs.menuTree.getCheckedKeys()
      const halfKeys = this.$refs.menuTree.getHalfCheckedKeys()
      const allKeys = [...checkedKeys, ...halfKeys]
      request.post(`/api/admin/role/menus/${this.currentRoleId}`, allKeys).then(() => {
        this.$message.success('菜单分配成功')
        this.menuVisible = false
      })
    }
  }
}
</script>
