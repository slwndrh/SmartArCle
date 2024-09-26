import express from 'express';
import admin from 'firebase-admin';
import bodyParser from 'body-parser';
import path from 'path';
import { fileURLToPath } from 'url';
import fs from 'fs/promises';
import cors from 'cors';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const serviceAccountPath = path.join(__dirname, 'config', 'serviceAccountKey.json');
const serviceAccount = JSON.parse(await fs.readFile(serviceAccountPath, 'utf8'));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

const app = express();
app.use(express.json());
app.use(bodyParser.json());
app.use(cors());

app.post('/post-token', async (req, res) => {
  const { email, token } = req.body;
  console.log(`Posting token for email: ${email} with token: ${token}`);
  if (!email || !token) {
    return res.status(400).json({ message: 'Missing email or token' });
  }

  try {
    await admin.firestore().collection('users').doc(email).set({ token: token }, { merge: true });
    res.status(200).json({ message: 'Token posted successfully', token: token });
  } catch (error) {
    console.error('Error updating token:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
});

app.get('/get-token/:email', async (req, res) => {
    const email = req.params.email;
    console.log(`Fetching token for email: ${email}`);
  
    try {
      const userDoc = await admin.firestore().collection('users').doc(email).get();
      if (!userDoc.exists) {
        return res.status(404).json({ message: 'User not found' });
      }
  
      const userToken = userDoc.data().token;
      return res.status(200).json({ token: userToken });
    } catch (error) {
      console.error('Error fetching user token:', error);
      return res.status(500).json({ message: 'Internal server error' });
    }
  });

  app.post('/report-missing', async (req, res) => {
    const { ownerName, plateNumber, location, securityEmail, ownerEmail } = req.body;
    console.log(`Request body: ${JSON.stringify(req.body)}`);

    if (!ownerName || !plateNumber || !location || !securityEmail || !ownerEmail) {
        return res.status(400).json({ message: 'Missing required fields' });
    }

    const [latitude, longitude] = location.split(',');
    if (!latitude || !longitude) {
        return res.status(400).json({ message: 'Invalid location format' });
    }

    try {
        const securityDoc = await admin.firestore().collection('security').doc(securityEmail).get();
        if (!securityDoc.exists) {
            return res.status(404).json({ message: 'Security email not found' });
        }
        const securityToken = securityDoc.data().token;

        const ownerDoc = await admin.firestore().collection('users').doc(ownerEmail).get();
        if (!ownerDoc.exists) {
            return res.status(404).json({ message: 'Owner email not found' });
        }
        const ownerToken = ownerDoc.data().token;

        if (!ownerToken) {
            console.log(`Owner token not found for email: ${ownerEmail}`);
            return res.status(404).json({ message: 'Owner token not found' });
        }

        const securityMessage = {
            token: securityToken,
            notification: {
                title: 'Vehicle Reported Missing',
                body: `Vehicle owned by ${ownerName} with license plate number ${plateNumber} has been stolen`
            },
            data: {
                latitude: latitude,
                longitude: longitude,
                location: `${latitude},${longitude}`,
                ownerName: ownerName,
                plateNumber: plateNumber,
                notificationType: 'VehicleReportedMissing',
                ownerEmail: ownerEmail,
                ownerToken: ownerToken
            }
        };

        await admin.messaging().send(securityMessage);
        console.log('Successfully sent message to security:', securityMessage);

        res.status(200).json({ message: 'Report sent to security and notification sent to owner' });
    } catch (error) {
        if (error.code === 'messaging/registration-token-not-registered') {
            console.error('Invalid token:', error);

            await admin.firestore().collection('users').doc(ownerEmail).update({ token: admin.firestore.FieldValue.delete() });
            res.status(400).json({ message: 'Invalid token, please update your token' });
        } else {
            console.error('Error processing report-missing request:', error);
            res.status(500).json({ message: 'Error processing report-missing request' });
        }
    }
});

app.post('/vehicle-found', async (req, res) => {
  const { securityEmail, ownerToken, message } = req.body;
  if (!securityEmail || !ownerToken || !message) {
      return res.status(400).json({ message: 'Missing required fields' });
  }

  try {
      const notificationMessage = {
          token: ownerToken,
          notification: {
              title: 'Vehicle Found',
              body: message
          },
          data: {
              notificationType: "ResponseToOwner"
          }
      };

      await admin.messaging().send(notificationMessage);
      console.log('Successfully sent message to owner');

      res.status(200).json({ message: 'Notification sent to owner successfully' });
  } catch (error) {
      console.error('Error sending notification to owner:', error);
      res.status(500).json({ message: 'Error sending notification to owner' });
  }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server berjalan pada http://192.168.50.47:${PORT}`);
});