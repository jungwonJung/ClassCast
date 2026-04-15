package com.example.classcast

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.classcast.adapter.ClassAdapter
import com.example.classcast.model.ClassItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ClassAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View
    private var snapshotListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // 로그인 여부 확인 — 비로그인 시 LoginActivity로 이동
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        // Toolbar 설정
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // View 초기화
        recyclerView = findViewById(R.id.recyclerView)
        emptyState = findViewById(R.id.emptyState)

        db = FirebaseFirestore.getInstance()

        // Adapter 설정 (onDelete, onStatusToggle 콜백)
        adapter = ClassAdapter(
            mutableListOf(),
            onDelete = { classItem -> showDeleteDialog(classItem) },
            onStatusToggle = { classItem, isActive -> toggleStatus(classItem, isActive) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // FAB 클릭 시 AddClassActivity로 이동
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            startActivity(Intent(this, AddClassActivity::class.java))
        }

        loadClasses()
    }

    /**
     * Firestore에서 현재 유저의 수업 목록을 실시간으로 불러옴 (Snapshot Listener)
     */
    private fun loadClasses() {
        val uid = auth.currentUser?.uid ?: return

        snapshotListener = db.collection("classes")
            .whereEqualTo("ownerId", uid)   // 본인 수업만 필터링
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                val classList = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(ClassItem::class.java)?.copy(classId = doc.id)
                } ?: emptyList()

                adapter.updateList(classList)

                // Empty state 처리 (Extension 3)
                if (classList.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                }
            }
    }

    /**
     * 삭제 확인 다이얼로그 표시
     */
    private fun showDeleteDialog(classItem: ClassItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_class))
            .setMessage("\"${classItem.courseName}\" 강의를 삭제하시겠습니까?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteClass(classItem)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Firestore에서 수업 삭제
     */
    private fun deleteClass(classItem: ClassItem) {
        db.collection("classes").document(classItem.classId).delete()
    }

    /**
     * 수업 상태 토글 (active ↔ inactive) — Extension 2: Status Toggle
     */
    private fun toggleStatus(classItem: ClassItem, isActive: Boolean) {
        val newStatus = if (isActive) "active" else "inactive"
        db.collection("classes")
            .document(classItem.classId)
            .update("status", newStatus)
    }

    // 상단 메뉴 (로그아웃)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                auth.signOut()
                goToLogin()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        snapshotListener?.remove()  // 메모리 누수 방지
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
