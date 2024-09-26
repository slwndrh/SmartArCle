const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.checkButtonAndAlarm = functions.database.ref('/Motor/{userEmail}')
    .onUpdate(async (change, context) => {
        // Ambil data sebelum dan sesudah update
        const before = change.before.val();
        const after = change.after.val();

        // Cek apakah Button dan Alarm bernilai 1
        if (after.Button === '1' && after.Alarm === 1) {
            const payload = {
                notification: {
                    title: 'Perhatian!',
                    body: `Button dan Alarm keduanya aktif!`,
                    clickAction: 'FLUTTER_NOTIFICATION_CLICK'
                }
            };

            // Ambil token dari Firestore berdasarkan email user
            const userEmail = context.params.userEmail.replace('_com', '@gmail.com');
            const userDoc = await admin.firestore().collection('user').doc(userEmail).get();

            if (userDoc.exists) {
                const userData = userDoc.data();
                const ownerToken = userData.token;

                // Kirim notifikasi ke token
                await admin.messaging().sendToDevice(ownerToken, payload);
                console.log('Notifikasi berhasil dikirim!');
            } else {
                console.log('Dokumen user tidak ditemukan!');
            }
        } else {
            console.log('Button dan Alarm tidak memenuhi kondisi.');
        }
        return null;
    });
