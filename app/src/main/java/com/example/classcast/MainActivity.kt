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

        // Redirect to login if not signed in
        if (auth.currentUser == null) {
            goToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.recyclerView)
        emptyState = findViewById(R.id.emptyState)

        db = FirebaseFirestore.getInstance()

        // Adapter with delete, status toggle, and headcount callbacks
        adapter = ClassAdapter(
            mutableListOf(),
            onDelete = { classItem -> showDeleteDialog(classItem) },
            onStatusToggle = { classItem, isActive -> toggleStatus(classItem, isActive) },
            onHeadcount = { classItem -> openHeadcount(classItem) }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // FAB opens AddClassActivity
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            startActivity(Intent(this, AddClassActivity::class.java))
        }

        loadClasses()
    }

    /**
     * Real-time Firestore listener — returns only the current user's classes.
     */
    private fun loadClasses() {
        val uid = auth.currentUser?.uid ?: return

        snapshotListener = db.collection("classes")
            .whereEqualTo("ownerId", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                val classList = snapshots?.documents?.mapNotNull { doc ->
                    doc.toObject(ClassItem::class.java)?.copy(classId = doc.id)
                } ?: emptyList()

                adapter.updateList(classList)

                // Extension 3: Empty state
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
     * Confirmation dialog before deleting a class.
     */
    private fun showDeleteDialog(classItem: ClassItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_class))
            .setMessage("Delete \"${classItem.courseName}\"?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                deleteClass(classItem)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Delete a class document from Firestore.
     */
    private fun deleteClass(classItem: ClassItem) {
        db.collection("classes").document(classItem.classId).delete()
    }

    /**
     * Toggle class status between active and inactive (Extension 2).
     */
    private fun toggleStatus(classItem: ClassItem, isActive: Boolean) {
        val newStatus = if (isActive) "active" else "inactive"
        db.collection("classes")
            .document(classItem.classId)
            .update("status", newStatus)
    }

    /**
     * Open the live headcount dashboard for the selected class.
     */
    private fun openHeadcount(classItem: ClassItem) {
        val intent = Intent(this, HeadcountActivity::class.java).apply {
            putExtra(HeadcountActivity.EXTRA_CLASS_ID, classItem.classId)
            putExtra(HeadcountActivity.EXTRA_COURSE_NAME, classItem.courseName)
            putExtra(HeadcountActivity.EXTRA_COURSE_CODE, classItem.courseCode)
        }
        startActivity(intent)
    }

    // Overflow menu: Student Vote + Logout
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_student_vote -> {
                startActivity(Intent(this, VoteActivity::class.java))
                true
            }
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
        snapshotListener?.remove() // Prevent memory leak
    }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
